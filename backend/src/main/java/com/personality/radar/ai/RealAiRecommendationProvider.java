package com.personality.radar.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.ai.client.DeepSeekClient;
import com.personality.radar.ai.client.MapClient;
import com.personality.radar.ai.data.CityRepository;
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
        if ("travel".equalsIgnoreCase(context.scene())) {
            return recommendTravel(context);
        }
        return recommendFood(context);
    }

    @Override
    public String name() {
        return "amap+deepseek";
    }

    private List<AiRecoItem> recommendFood(AiRecoContext context) {
        Double lat = context.lat();
        Double lng = context.lng();
        if ((lat == null || lng == null) && context.city() != null && !context.city().isBlank()) {
            double[] resolved = mapClient.geocodeCity(context.city().trim());
            lat = resolved[0];
            lng = resolved[1];
        }
        if (lat == null || lng == null) {
            throw new IllegalStateException("Missing coordinates or city for food recommendation");
        }

        List<RestaurantCandidate> candidates = mapClient.nearbyRestaurants(lat, lng);
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("AMap returned no restaurant candidates");
        }

        try {
            String prompt = buildFoodPrompt(context.profileScores(), candidates, context.limit());
            String raw = deepSeekClient.chat(prompt);
            List<AiRecoItem> ranked = parseFoodResults(raw, candidates, context.limit());
            if (!ranked.isEmpty()) {
                return ranked;
            }
            log.warn("AI food output had no usable restaurant; using real POI fallback");
        } catch (Exception e) {
            log.warn("AI food ranking failed; using real POI fallback: {}", e.toString());
        }

        return rankFoodCandidates(context.profileScores(), candidates, context.limit());
    }

    private List<AiRecoItem> recommendTravel(AiRecoContext context) {
        List<String> cities = chooseTravelCities(context);
        List<TravelBundle> bundles = new ArrayList<>();
        for (String city : cities) {
            List<RestaurantCandidate> pois = mapClient.travelPois(city, 18);
            if (!pois.isEmpty()) {
                bundles.add(new TravelBundle(city, pois));
            }
            if (bundles.size() >= Math.max(1, Math.min(context.limit(), 4))) {
                break;
            }
        }
        if (bundles.isEmpty()) {
            throw new IllegalStateException("AMap returned no travel candidates");
        }

        try {
            String prompt = buildTravelPrompt(context.profileScores(), bundles, context.limit());
            String raw = deepSeekClient.chat(prompt);
            List<AiRecoItem> ranked = parseTravelResults(raw, bundles, context.limit());
            if (!ranked.isEmpty()) {
                return fillTravelResults(context.profileScores(), bundles, ranked, context.limit());
            }
            log.warn("AI travel output had no usable route; using real POI fallback");
        } catch (Exception e) {
            log.warn("AI travel ranking failed; using real POI fallback: {}", e.toString());
        }
        return fallbackTravelResults(context.profileScores(), bundles, context.limit());
    }

    private List<String> chooseTravelCities(AiRecoContext context) {
        String city = context.city() == null ? "" : context.city().trim();
        List<String> pool = CityRepository.getCities();
        List<String> selected = new ArrayList<>();
        if (!city.isBlank()) {
            selected.add(city);
            return selected;
        }
        int adventure = score(context.profileScores(), PersonalityDimension.TRAVEL_ADVENTURE);
        int planning = score(context.profileScores(), PersonalityDimension.TRAVEL_PLANNING);
        List<String> ranked = pool.stream()
                .sorted(Comparator.comparingInt((String c) -> cityScore(c, adventure, planning)).reversed())
                .toList();
        for (String candidate : ranked) {
            if (!selected.contains(candidate)) {
                selected.add(candidate);
            }
            if (selected.size() >= 3) {
                break;
            }
        }
        return selected;
    }

    private int cityScore(String city, int adventure, int planning) {
        int result = 60;
        if (List.of("\u5927\u7406", "\u4e3d\u6c5f", "\u91cd\u5e86", "\u53a6\u95e8", "\u4e09\u4e9a").contains(city)) {
            result += (adventure - 50) / 3;
        }
        if (List.of("\u5317\u4eac", "\u4e0a\u6d77", "\u676d\u5dde", "\u5357\u4eac", "\u82cf\u5dde").contains(city)) {
            result += (planning - 50) / 3;
        }
        if (List.of("\u6210\u90fd", "\u957f\u6c99", "\u6b66\u6c49", "\u9752\u5c9b").contains(city)) {
            result += 6;
        }
        return result;
    }

    private String buildFoodPrompt(Map<String, Integer> scores, List<RestaurantCandidate> list, int limit) {
        int count = Math.min(Math.max(limit, 1), Math.min(list.size(), 8));
        StringBuilder sb = new StringBuilder();
        sb.append("Select ").append(count).append(" restaurants from candidates and rank them for the user.\n");
        sb.append("Return JSON only: {\"results\":[{\"name\":\"exact candidate name\",\"score\":88,\"reason\":\"short Chinese reason\"}]}\n");
        sb.append(profileText(scores));
        sb.append("Candidates:\n");
        for (int i = 0; i < Math.min(list.size(), 12); i++) {
            RestaurantCandidate r = list.get(i);
            sb.append(i + 1).append(". ").append(r.getName())
                    .append(" | ").append(valueOrDash(r.getCategory()))
                    .append(" | ").append(valueOrDash(r.getAddress()))
                    .append(" | distance=").append(r.getDistance() == null ? "unknown" : Math.round(r.getDistance()) + "m")
                    .append(" | rating=").append(r.getRating() == null ? "unknown" : r.getRating())
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildTravelPrompt(Map<String, Integer> scores, List<TravelBundle> bundles, int limit) {
        int count = Math.min(Math.max(limit, 1), bundles.size());
        StringBuilder sb = new StringBuilder();
        sb.append("You are a travel recommendation API. Choose ").append(count).append(" city travel plans for the user.\n");
        sb.append("Use only the given city names and POI names. name must be a real POI from that city.\n");
        sb.append("Return JSON only in this exact shape: ");
        sb.append("{\"results\":[{\"city\":\"城市\",\"name\":\"POI名\",\"category\":\"2日城市漫游\",\"score\":88,\"reason\":\"为什么适合\",\"durationDays\":2,\"highlights\":[\"亮点1\",\"亮点2\"],\"itinerary\":[\"上午...\",\"下午...\"],\"tips\":[\"建议1\"]}]}\n");
        sb.append(profileText(scores));
        sb.append("Travel candidates:\n");
        for (TravelBundle bundle : bundles) {
            sb.append("City: ").append(bundle.city()).append("\n");
            for (int i = 0; i < Math.min(bundle.pois().size(), 10); i++) {
                RestaurantCandidate p = bundle.pois().get(i);
                sb.append("- ").append(p.getName())
                        .append(" | ").append(valueOrDash(p.getCategory()))
                        .append(" | ").append(valueOrDash(p.getAddress()))
                        .append("\n");
            }
        }
        return sb.toString();
    }

    private String profileText(Map<String, Integer> scores) {
        return "Profile:\n"
                + "- openness=" + score(scores, PersonalityDimension.OPENNESS) + "/100\n"
                + "- extraversion=" + score(scores, PersonalityDimension.EXTRAVERSION) + "/100\n"
                + "- foodAdventure=" + score(scores, PersonalityDimension.FOOD_ADVENTURE) + "/100\n"
                + "- foodSocial=" + score(scores, PersonalityDimension.FOOD_SOCIAL) + "/100\n"
                + "- travelAdventure=" + score(scores, PersonalityDimension.TRAVEL_ADVENTURE) + "/100\n"
                + "- travelPlanning=" + score(scores, PersonalityDimension.TRAVEL_PLANNING) + "/100\n";
    }

    private List<AiRecoItem> parseFoodResults(String raw, List<RestaurantCandidate> candidates, int limit) {
        Map<String, RestaurantCandidate> byName = byName(candidates);
        List<AiRecoItem> result = new ArrayList<>();
        try {
            JsonNode results = mapper.readTree(extractJsonObject(raw)).get("results");
            if (results == null || !results.isArray()) {
                return result;
            }
            for (JsonNode node : results) {
                String name = text(node, "name");
                RestaurantCandidate candidate = byName.get(name);
                if (candidate == null) {
                    continue;
                }
                result.add(toFoodItem(candidate, clamp(node.has("score") ? node.get("score").asInt(70) : 70), text(node, "reason")));
                if (result.size() >= Math.max(1, limit)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI food output: {}", e.toString());
        }
        return result;
    }

    private List<AiRecoItem> parseTravelResults(String raw, List<TravelBundle> bundles, int limit) {
        Map<String, TravelPoi> byCityAndName = new LinkedHashMap<>();
        for (TravelBundle bundle : bundles) {
            for (RestaurantCandidate poi : bundle.pois()) {
                byCityAndName.put(bundle.city() + "::" + poi.getName(), new TravelPoi(bundle.city(), poi));
            }
        }
        List<AiRecoItem> result = new ArrayList<>();
        try {
            JsonNode results = mapper.readTree(extractJsonObject(raw)).get("results");
            if (results == null || !results.isArray()) {
                return result;
            }
            for (JsonNode node : results) {
                String city = text(node, "city");
                String name = text(node, "name");
                TravelPoi travelPoi = byCityAndName.get(city + "::" + name);
                if (travelPoi == null) {
                    continue;
                }
                result.add(toTravelItem(
                        travelPoi.city(),
                        travelPoi.poi(),
                        clamp(node.has("score") ? node.get("score").asInt(72) : 72),
                        text(node, "category"),
                        text(node, "reason"),
                        node.has("durationDays") ? node.get("durationDays").asInt(2) : 2,
                        stringList(node.get("highlights")),
                        stringList(node.get("itinerary")),
                        stringList(node.get("tips"))));
                if (result.size() >= Math.max(1, limit)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI travel output: {}", e.toString());
        }
        return result;
    }

    private List<AiRecoItem> rankFoodCandidates(Map<String, Integer> scores, List<RestaurantCandidate> candidates, int limit) {
        int adventure = score(scores, PersonalityDimension.FOOD_ADVENTURE);
        int social = score(scores, PersonalityDimension.FOOD_SOCIAL);
        return candidates.stream()
                .filter(c -> c.getName() != null && !c.getName().isBlank())
                .sorted(Comparator.comparingDouble((RestaurantCandidate c) -> foodScore(c, adventure, social)).reversed())
                .limit(Math.max(1, limit))
                .map(c -> toFoodItem(c, clamp((int) Math.round(foodScore(c, adventure, social))), foodFallbackReason(c, adventure, social)))
                .toList();
    }

    private List<AiRecoItem> fillTravelResults(
            Map<String, Integer> scores,
            List<TravelBundle> bundles,
            List<AiRecoItem> ranked,
            int limit) {
        if (ranked.size() >= Math.max(1, limit)) {
            return ranked;
        }
        List<AiRecoItem> filled = new ArrayList<>(ranked);
        List<String> used = ranked.stream().map(AiRecoItem::name).toList();
        for (AiRecoItem item : fallbackTravelResults(scores, bundles, limit)) {
            if (!used.contains(item.name())) {
                filled.add(item);
            }
            if (filled.size() >= Math.max(1, limit)) {
                break;
            }
        }
        return filled;
    }
    private List<AiRecoItem> fallbackTravelResults(Map<String, Integer> scores, List<TravelBundle> bundles, int limit) {
        int adventure = score(scores, PersonalityDimension.TRAVEL_ADVENTURE);
        int planning = score(scores, PersonalityDimension.TRAVEL_PLANNING);
        List<AiRecoItem> result = new ArrayList<>();
        for (TravelBundle bundle : bundles) {
            List<String> cityHighlights = bundle.pois().stream().limit(4).map(RestaurantCandidate::getName).toList();
            for (RestaurantCandidate poi : bundle.pois()) {
                result.add(toTravelItem(
                        bundle.city(),
                        poi,
                        clamp(72 + (adventure - 50) / 5 + (planning - 50) / 8 - result.size()),
                        planning >= 60 ? "计划型同城景点" : "探索型同城景点",
                        travelFallbackReason(bundle.city(), adventure, planning),
                        planning >= 60 ? 2 : 3,
                        cityHighlights,
                        List.of("优先游览 " + poi.getName(), "结合周边街区或餐饮安排半日动线"),
                        List.of(planning >= 60 ? "提前确认开放时间和预约要求" : "保留临场调整时间，避免行程过满")));
                if (result.size() >= Math.max(1, limit)) {
                    return result;
                }
            }
        }
        return result;
    }

    private double foodScore(RestaurantCandidate c, int adventure, int social) {
        double result = 68;
        if (c.getRating() != null) {
            result += Math.max(0, c.getRating() - 3.5) * 8;
        }
        if (c.getDistance() != null) {
            result += Math.max(0, 5000 - c.getDistance()) / 500;
        }
        String category = c.getCategory() == null ? "" : c.getCategory();
        if (adventure >= 60 && (category.contains("\u5f02\u56fd") || category.contains("\u897f\u9910") || category.contains("\u65e5\u672c") || category.contains("\u97e9\u56fd") || category.contains("\u521b\u610f"))) {
            result += 8;
        }
        if (social >= 60 && (category.contains("\u706b\u9505") || category.contains("\u70e7\u70e4") || category.contains("\u9152\u5427") || category.contains("\u5496\u5561"))) {
            result += 8;
        }
        return Math.min(result, 96);
    }

    private AiRecoItem toFoodItem(RestaurantCandidate c, int score, String reason) {
        return new AiRecoItem(
                c.getName(), c.getCategory(), c.getAddress(), c.getDistance(), c.getRating(), c.getPriceLevel(),
                score, usefulReason(reason, foodFallbackReason(c, 50, 50)), tagsForFood(c), buildMapUrl(c), c.getLocation(),
                null, null, null, null, null);
    }

    private AiRecoItem toTravelItem(
            String city,
            RestaurantCandidate poi,
            int score,
            String category,
            String reason,
            Integer durationDays,
            List<String> highlights,
            List<String> itinerary,
            List<String> tips) {
        List<String> usableHighlights = highlights == null || highlights.isEmpty() ? List.of(poi.getName()) : highlights;
        return new AiRecoItem(
                poi.getName(),
                usefulReason(category, "城市旅行计划"),
                poi.getAddress(),
                poi.getDistance(),
                poi.getRating(),
                poi.getPriceLevel(),
                score,
                usefulReason(reason, travelFallbackReason(city, 50, 50)),
                tagsForTravel(city, durationDays, usableHighlights),
                buildMapUrl(poi),
                poi.getLocation(),
                city,
                durationDays,
                usableHighlights,
                itinerary,
                tips);
    }

    private String foodFallbackReason(RestaurantCandidate c, int adventure, int social) {
        StringBuilder reason = new StringBuilder();
        if (c.getDistance() != null) {
            reason.append("距离约").append(Math.round(c.getDistance())).append("米，");
        }
        if (c.getRating() != null) {
            reason.append("评分").append(c.getRating()).append("，");
        }
        reason.append(adventure >= social ? "适合想尝试不同口味的一餐。" : "适合和朋友轻松聚餐。");
        return reason.toString();
    }

    private String travelFallbackReason(String city, int adventure, int planning) {
        if (planning >= adventure) {
            return city + "的交通和景点密度适合做成清晰的短途计划，执行成本低。";
        }
        return city + "有足够多元的街区和目的地，适合保留弹性的探索型旅行。";
    }

    private List<String> tagsForFood(RestaurantCandidate c) {
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

    private List<String> tagsForTravel(String city, Integer durationDays, List<String> highlights) {
        List<String> tags = new ArrayList<>();
        tags.add(city);
        if (durationDays != null) {
            tags.add(durationDays + "天");
        }
        if (highlights != null && highlights.size() >= 3) {
            tags.add("多点路线");
        }
        return tags;
    }

    private Map<String, RestaurantCandidate> byName(List<RestaurantCandidate> candidates) {
        Map<String, RestaurantCandidate> byName = new LinkedHashMap<>();
        for (RestaurantCandidate c : candidates) {
            if (c.getName() != null && !c.getName().isBlank()) {
                byName.putIfAbsent(c.getName(), c);
            }
        }
        return byName;
    }

    private String extractJsonObject(String raw) {
        String cleaned = raw == null ? "" : raw.replace("```json", "").replace("```", "").trim();
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
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText(null);
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String usefulReason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int score(Map<String, Integer> scores, PersonalityDimension dimension) {
        return scores.getOrDefault(dimension.name(), 50);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record TravelBundle(String city, List<RestaurantCandidate> pois) {
    }

    private record TravelPoi(String city, RestaurantCandidate poi) {
    }
}