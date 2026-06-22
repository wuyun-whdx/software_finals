package com.personality.radar.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.ai.client.DeepSeekClient;
import com.personality.radar.ai.client.MapClient;
import com.personality.radar.ai.dto.RestaurantCandidate;
import com.personality.radar.domain.PersonalityDimension;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "real")
public class RealAiRecommendationProvider implements AiRecommendationProvider {
    private static final Logger log = LoggerFactory.getLogger(RealAiRecommendationProvider.class);

    private final MapClient mapClient;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper mapper;

    public RealAiRecommendationProvider(MapClient mapClient, DeepSeekClient deepSeekClient, ObjectMapper mapper) {
        this.mapClient = mapClient;
        this.deepSeekClient = deepSeekClient;
        this.mapper = mapper;
    }

    @Override
    public List<AiRecoItem> recommend(AiRecoContext context) {
        Double lat = context.lat();
        Double lng = context.lng();
        if ((lat == null || lng == null) && context.city() != null && !context.city().isBlank()) {
            double[] resolved = mapClient.geocodeCity(context.city().trim());
            lat = resolved[0];
            lng = resolved[1];
        }
        if (lat == null || lng == null) {
            throw new IllegalStateException("缺少经纬度或城市，real provider 无法调用地图");
        }

        List<RestaurantCandidate> candidates = mapClient.nearbyRestaurants(lat, lng);
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("地图未返回任何候选餐厅");
        }

        try {
            String prompt = buildPrompt(context.profileScores(), candidates, context.limit());
            String raw = deepSeekClient.chat(prompt);
            List<AiRecoItem> ranked = parseAndFilter(raw, candidates, context.limit());
            if (!ranked.isEmpty()) {
                return ranked;
            }
            log.warn("AI 输出没有可用餐厅，改用真实地图候选排序");
        } catch (Exception e) {
            log.warn("AI 排序失败，改用真实地图候选排序: {}", e.toString());
        }

        return rankRealCandidates(context.profileScores(), candidates, context.limit());
    }

    @Override
    public String name() {
        return "amap+deepseek";
    }

    private String buildPrompt(Map<String, Integer> scores, List<RestaurantCandidate> list, int limit) {
        int count = Math.min(Math.max(limit, 1), Math.min(list.size(), 8));
        StringBuilder sb = new StringBuilder();
        sb.append("请从候选餐厅中选择最适合用户的 ").append(count).append(" 家，并按适合度排序。\n");
        sb.append("只能选择候选列表里存在的餐厅，name 必须完全照抄。\n");
        sb.append("输出必须是 JSON 对象，格式：{\"results\":[{\"name\":\"餐厅名\",\"score\":88,\"reason\":\"一句具体推荐理由\"}]}\n\n");
        sb.append("用户画像：\n");
        sb.append(profileLine(scores, PersonalityDimension.FOOD_ADVENTURE, "饮食探索"));
        sb.append(profileLine(scores, PersonalityDimension.FOOD_SOCIAL, "饮食社交"));
        sb.append(profileLine(scores, PersonalityDimension.EXTRAVERSION, "外向"));
        sb.append(profileLine(scores, PersonalityDimension.OPENNESS, "开放"));
        sb.append("\n候选餐厅：\n");
        for (int i = 0; i < Math.min(list.size(), 12); i++) {
            RestaurantCandidate r = list.get(i);
            sb.append(i + 1).append(". ").append(r.getName())
                    .append(" | ").append(valueOrDash(r.getCategory()))
                    .append(" | ").append(valueOrDash(r.getAddress()))
                    .append(" | 距离").append(r.getDistance() == null ? "未知" : Math.round(r.getDistance()) + "m")
                    .append(" | 评分").append(r.getRating() == null ? "未知" : r.getRating())
                    .append(" | ").append(valueOrDash(r.getPriceLevel()))
                    .append("\n");
        }
        return sb.toString();
    }

    private String profileLine(Map<String, Integer> scores, PersonalityDimension dim, String desc) {
        int value = scores.getOrDefault(dim.name(), 50);
        return "- " + desc + ": " + value + "/100\n";
    }

    private List<AiRecoItem> parseAndFilter(String raw, List<RestaurantCandidate> candidates, int limit) {
        String cleaned = extractJsonObject(raw == null ? "" : raw.trim());

        Map<String, RestaurantCandidate> byName = new LinkedHashMap<>();
        for (RestaurantCandidate c : candidates) {
            if (c.getName() != null && !c.getName().isBlank()) {
                byName.putIfAbsent(c.getName(), c);
            }
        }

        List<AiRecoItem> result = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(cleaned);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return result;
            }
            for (JsonNode node : results) {
                String name = text(node, "name");
                if (name == null || !byName.containsKey(name)) {
                    continue;
                }
                RestaurantCandidate candidate = byName.get(name);
                int score = clamp(node.has("score") ? node.get("score").asInt(70) : 70);
                String reason = text(node, "reason");
                result.add(toRecoItem(candidate, score, usefulReason(candidate, reason)));
                if (result.size() >= Math.max(1, limit)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("解析 AI 输出失败: {}", e.toString());
        }
        return result;
    }

    private List<AiRecoItem> rankRealCandidates(
            Map<String, Integer> scores, List<RestaurantCandidate> candidates, int limit) {
        int adventure = scores.getOrDefault(PersonalityDimension.FOOD_ADVENTURE.name(), 50);
        int social = scores.getOrDefault(PersonalityDimension.FOOD_SOCIAL.name(), 50);
        return candidates.stream()
                .filter(c -> c.getName() != null && !c.getName().isBlank())
                .sorted(Comparator.comparingDouble((RestaurantCandidate c) -> localScore(c, adventure, social)).reversed())
                .limit(Math.max(1, limit))
                .map(c -> toRecoItem(c, clamp((int) Math.round(localScore(c, adventure, social))), fallbackReason(c, adventure, social)))
                .toList();
    }

    private double localScore(RestaurantCandidate c, int adventure, int social) {
        double score = 68;
        if (c.getRating() != null) {
            score += Math.max(0, c.getRating() - 3.5) * 8;
        }
        if (c.getDistance() != null) {
            score += Math.max(0, 5000 - c.getDistance()) / 500;
        }
        String category = c.getCategory() == null ? "" : c.getCategory();
        if (adventure >= 60 && (category.contains("异国") || category.contains("西餐") || category.contains("日本") || category.contains("韩国") || category.contains("创意"))) {
            score += 8;
        }
        if (social >= 60 && (category.contains("火锅") || category.contains("烧烤") || category.contains("酒吧") || category.contains("咖啡"))) {
            score += 8;
        }
        return Math.min(score, 96);
    }

    private AiRecoItem toRecoItem(RestaurantCandidate c, int score, String reason) {
        return new AiRecoItem(
                c.getName(),
                c.getCategory(),
                c.getAddress(),
                c.getDistance(),
                c.getRating(),
                c.getPriceLevel(),
                score,
                reason,
                tagsFor(c),
                buildMapUrl(c),
                c.getLocation());
    }

    private String fallbackReason(RestaurantCandidate c, int adventure, int social) {
        StringBuilder reason = new StringBuilder();
        if (c.getDistance() != null) {
            reason.append("距离约").append(Math.round(c.getDistance())).append("米，");
        }
        if (c.getRating() != null) {
            reason.append("评分").append(c.getRating()).append("，");
        }
        if (adventure >= social) {
            reason.append("适合想尝试不同口味的一餐。");
        } else {
            reason.append("适合和朋友轻松聚餐。");
        }
        return reason.toString();
    }

    private String usefulReason(RestaurantCandidate c, String aiReason) {
        if (aiReason != null && !aiReason.isBlank()) {
            return aiReason;
        }
        return fallbackReason(c, 50, 50);
    }

    private List<String> tagsFor(RestaurantCandidate c) {
        List<String> tags = new ArrayList<>();
        if (c.getDistance() != null && c.getDistance() <= 1000) {
            tags.add("附近");
        }
        if (c.getRating() != null && c.getRating() >= 4.5) {
            tags.add("高评分");
        }
        if (c.getPriceLevel() != null) {
            tags.add(c.getPriceLevel());
        }
        return tags.isEmpty() ? null : tags;
    }

    private String extractJsonObject(String raw) {
        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String buildMapUrl(RestaurantCandidate c) {
        if (c.getLocation() == null || c.getLocation().isBlank()) {
            return null;
        }
        String name = c.getName() == null ? "" : URLEncoder.encode(c.getName(), StandardCharsets.UTF_8);
        return "https://uri.amap.com/marker?position=" + c.getLocation() + "&name=" + name;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}