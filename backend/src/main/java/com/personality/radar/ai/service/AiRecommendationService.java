package com.personality.radar.ai.service;

import com.personality.radar.ai.client.DeepSeekClient;
import com.personality.radar.ai.client.MapClient;
import com.personality.radar.ai.dto.RestaurantCandidate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.ai.dto.AiRankItem;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRecommendationService {

    private final MapClient mapClient;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiRecommendationService(MapClient mapClient,
                                    DeepSeekClient deepSeekClient) {
        this.mapClient = mapClient;
        this.deepSeekClient = deepSeekClient;
    }

    /**
     * 主入口：推荐餐厅
     */
    public List<RestaurantCandidate> recommend(double lat, double lng) {

        // 1. 拉取候选餐厅
        List<RestaurantCandidate> candidates =
                mapClient.nearbyRestaurants(lat, lng);

        // 2. 构造严格 JSON Prompt
        String prompt = buildPrompt(candidates);

        // 3. 调用 AI
        String aiResult = deepSeekClient.chat(prompt);

        System.out.println("=== AI RAW OUTPUT ===");
        System.out.println(aiResult);

        // 清理 AI 输出（防 ```json / 多余文字）
        aiResult = aiResult.trim();

        if (aiResult.contains("```")) {
            aiResult = aiResult
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
        }

        try {
            // 4. 解析 AI JSON
            Map<String, Object> response = objectMapper.readValue(
                    aiResult,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");

            if (results == null) {
                return candidates;
            }

            List<String> orderedNames = results.stream()
                    .map(r -> (String) r.get("name"))
                    .toList();

            // 6. 映射回真实餐厅 + 防幻觉过滤
            Map<String, RestaurantCandidate> candidateMap =
                    candidates.stream()
                            .collect(Collectors.toMap(
                                    RestaurantCandidate::getName,
                                    c -> c,
                                    (a, b) -> a
                            ));

            List<RestaurantCandidate> ranked = new ArrayList<>();

            for (String name : orderedNames) {
                if (candidateMap.containsKey(name)) {
                    ranked.add(candidateMap.get(name));
                }
            }

            // 7. 如果AI失败就兜底
            if (ranked.isEmpty()) {
                return candidates;
            }

            return ranked;

        } catch (Exception e) {
            e.printStackTrace();
            return candidates;
        }
    }

    /**
     * 构造 Prompt（当前最关键调试点）
     */
    private String buildPrompt(List<RestaurantCandidate> list) {

        StringBuilder sb = new StringBuilder();

        sb.append("你是一个本地餐厅推荐助手，请根据用户偏好对餐厅进行排序，并给出推荐理由。\n\n");

        sb.append("餐厅列表如下：\n");

        for (int i = 0; i < list.size(); i++) {
            RestaurantCandidate r = list.get(i);

            sb.append(i + 1)
                    .append(". ")
                    .append(r.getName())
                    .append(" | ")
                    .append(r.getAddress())
                    .append(" | 距离:")
                    .append(r.getDistance())
                    .append("m\n");
        }

        sb.append("""
        你是API系统，只能输出JSON。

        禁止：
        - 解释
        - Markdown
        - ```json
        - 多余文字

        如果不符合格式，你将被程序拒绝。

        必须严格输出：

        {
        "results": [
            {"name": "餐厅名"}
        ]
        }
        """);

        return sb.toString();
    }
}