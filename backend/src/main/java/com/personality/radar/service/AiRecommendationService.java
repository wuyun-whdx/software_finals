package com.personality.radar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.config.AiProperties;
import com.personality.radar.domain.SceneType;
import com.personality.radar.domain.UserAccount;
import com.personality.radar.dto.ApiDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);
    private final AiClient aiClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    private final RecommendationService recommendationService;

    public AiRecommendationService(AiClient aiClient, AiProperties aiProperties,
                                   ObjectMapper objectMapper, StringRedisTemplate redis,
                                   @Lazy RecommendationService recommendationService) {
        this.aiClient = aiClient;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.recommendationService = recommendationService;
    }

    public List<ApiDtos.LocationRecommendationResponse> recommend(
            UserAccount user, SceneType scene, String province, String city, String district) {

        String cacheKey = String.format("ai:rec:%d:%s:%s:%s:%s",
                user.getId(), scene.name().toLowerCase(), province, city,
                district != null ? district : "");

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<ApiDtos.LocationRecommendationResponse>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse cached AI recommendation: {}", e.getMessage());
            }
        }

        Map<String, Integer> scores = recommendationService.getMergedScores(user);
        String sceneLabel = switch (scene) {
            case FOOD -> "饮食推荐";
            case TRAVEL -> "旅游推荐";
            case SOCIAL -> "社交推荐";
            case OUTFIT -> "穿搭推荐";
            case CAREER -> "生涯推荐";
        };

        String fullRegion = province + " " + city + (district != null ? " " + district : "");

        String systemPrompt = "你是一个精准的本地生活推荐助手。根据用户信息推荐5个位于指定城市的真实线下地点。请严格返回JSON数组，每个元素包含: title(地点名称), address(详细地址), reason(300字以内推荐理由), tags(标签数组)。只返回JSON，不要任何额外文字。";

        String userMessage = String.format("""
                用户性格画像（10维度分数）: %s
                推荐场景: %s
                所在城市: %s

                请为该用户推荐5个%s的真实线下地点。""",
                scores.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ")),
                sceneLabel, fullRegion, fullRegion);

        String raw = aiClient.chat(systemPrompt, userMessage);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }
            List<ApiDtos.LocationRecommendationResponse> list =
                    objectMapper.readValue(json, new TypeReference<List<ApiDtos.LocationRecommendationResponse>>() {});
            if (list != null && !list.isEmpty()) {
                redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(list),
                        Duration.ofHours(aiProperties.getCacheTtlHours()));
            }
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            return List.of();
        }
    }
}
