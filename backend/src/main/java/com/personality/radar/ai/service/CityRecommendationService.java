package com.personality.radar.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.ai.client.DeepSeekClient;
import com.personality.radar.ai.data.CityRepository;
import com.personality.radar.ai.dto.CityCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CityRecommendationService {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CityRecommendationService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public List<CityCandidate> recommend(
            double openness,
            double conscientiousness,
            double extraversion,
            double agreeableness,
            double emotionalStability,
            double foodExplore,
            double foodSocial,
            double travelExplore,
            double travelPlan,
            double socialEnergy) {

        List<String> cities = CityRepository.getCities();
        String prompt = buildPrompt(cities, openness, conscientiousness, extraversion, agreeableness,
                emotionalStability, foodExplore, foodSocial, travelExplore, travelPlan, socialEnergy);

        try {
            String result = deepSeekClient.chat(prompt);
            JsonNode results = objectMapper.readTree(extractJsonObject(result)).get("results");
            Map<String, CityCandidate> cityMap = new LinkedHashMap<>();
            for (String city : cities) {
                CityCandidate candidate = new CityCandidate();
                candidate.setName(city);
                candidate.setScore(60);
                candidate.setReason("候选城市");
                cityMap.put(city, candidate);
            }

            List<CityCandidate> ranked = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    String name = node.path("name").asText();
                    CityCandidate candidate = cityMap.remove(name);
                    if (candidate != null) {
                        candidate.setScore(node.path("score").asInt(70));
                        candidate.setReason(node.path("reason").asText("适合当前旅行画像"));
                        ranked.add(candidate);
                    }
                }
            }
            ranked.addAll(cityMap.values());
            return ranked;
        } catch (Exception e) {
            return cities.stream().map(city -> {
                CityCandidate candidate = new CityCandidate();
                candidate.setName(city);
                candidate.setScore(60);
                candidate.setReason("城市候选池兜底推荐");
                return candidate;
            }).toList();
        }
    }

    private String buildPrompt(
            List<String> cities,
            double openness,
            double conscientiousness,
            double extraversion,
            double agreeableness,
            double emotionalStability,
            double foodExplore,
            double foodSocial,
            double travelExplore,
            double travelPlan,
            double socialEnergy) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rank travel cities for this user. Only use provided city names. Return JSON only.\n");
        sb.append("Format: {\"results\":[{\"name\":\"城市\",\"score\":88,\"reason\":\"简短理由\"}]}\n");
        sb.append("Profile: openness=").append(openness)
                .append(", conscientiousness=").append(conscientiousness)
                .append(", extraversion=").append(extraversion)
                .append(", agreeableness=").append(agreeableness)
                .append(", emotionalStability=").append(emotionalStability)
                .append(", foodExplore=").append(foodExplore)
                .append(", foodSocial=").append(foodSocial)
                .append(", travelExplore=").append(travelExplore)
                .append(", travelPlan=").append(travelPlan)
                .append(", socialEnergy=").append(socialEnergy)
                .append("\nCities:\n");
        for (String city : cities) {
            sb.append("- ").append(city).append("\n");
        }
        return sb.toString();
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
}