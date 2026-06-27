package com.personality.radar.service;

import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.Feedback;
import com.personality.radar.domain.FeedbackRating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.personality.radar.domain.PersonalityDimension;
import com.personality.radar.domain.RecommendationItem;
import com.personality.radar.domain.RecommendationRule;
import com.personality.radar.domain.SceneType;
import com.personality.radar.domain.TestResult;
import com.personality.radar.domain.TestType;
import com.personality.radar.domain.UserAccount;
import com.personality.radar.domain.UserPreference;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.repository.FeedbackRepository;
import com.personality.radar.repository.RecommendationItemRepository;
import com.personality.radar.repository.RecommendationRuleRepository;
import com.personality.radar.repository.TestResultRepository;
import com.personality.radar.repository.UserPreferenceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private final RecommendationItemRepository items;
    private final TestResultRepository results;
    private final FeedbackRepository feedbacks;
    private final UserPreferenceRepository preferences;
    private final RecommendationRuleRepository rules;
    private final AiRecommendationService aiRecommendationService;

    public RecommendationService(
            RecommendationItemRepository items,
            TestResultRepository results,
            FeedbackRepository feedbacks,
            UserPreferenceRepository preferences,
            RecommendationRuleRepository rules,
            AiRecommendationService aiRecommendationService) {
        this.items = items;
        this.results = results;
        this.feedbacks = feedbacks;
        this.preferences = preferences;
        this.rules = rules;
        this.aiRecommendationService = aiRecommendationService;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.RecommendationResponse> recommend(UserAccount user, String sceneValue) {
        return recommendWithRegion(user, sceneValue, null, null, null).stream()
                .map(r -> new ApiDtos.RecommendationResponse(
                        r.id(), r.scene(), r.title(), r.description(), r.tags(),
                        r.score(), r.baseScore(), r.active()))
                .toList();
    }

    @Transactional
    public List<ApiDtos.LocationRecommendationResponse> recommendWithRegion(
            UserAccount user, String sceneValue,
            String province, String city, String district) {

        SceneType scene = EnumParser.sceneType(sceneValue);
        Map<String, Integer> mergedScores = mergedLatestScores(user);
        Map<String, Integer> preferenceMap = preferences.findByUser(user).stream()
                .collect(Collectors.toMap(UserPreference::getTag, UserPreference::getWeight));
        Map<String, Integer> ruleMap = rules.findByActiveTrueOrderByTagAsc().stream()
                .collect(Collectors.toMap(RecommendationRule::getTag, RecommendationRule::getWeight));

        List<ApiDtos.RecommendationResponse> general = items.findBySceneAndActiveTrue(scene).stream()
                .map(item -> DtoMapper.recommendation(item,
                        RecommendationRanker.score(item.getBaseScore(), item.getTags(), preferenceMap, mergedScores, ruleMap)))
                .sorted(Comparator.comparing(ApiDtos.RecommendationResponse::score).reversed())
                .limit(10)
                .collect(Collectors.toCollection(ArrayList::new));

        List<ApiDtos.LocationRecommendationResponse> merged = new ArrayList<>();
        if (province != null && !province.isBlank() && city != null && !city.isBlank()) {
            try {
                List<ApiDtos.LocationRecommendationResponse> ai = aiRecommendationService.recommend(
                        user, scene, province, city, district);
                // Ensure AI items have source and scene set
                for (int idx = 0; idx < ai.size(); idx++) {
                    var item = ai.get(idx);
                    if (item.source() == null || item.source().isBlank()) {
                        ai.set(idx, new ApiDtos.LocationRecommendationResponse(
                                (long) -(idx + 1), scene.name().toLowerCase(), item.title(),
                                item.description(), item.tags(), item.score(),
                                item.baseScore(), true, item.address(),
                                item.aiReason(), "ai"));
                    }
                }
                merged.addAll(ai);
            } catch (Exception e) {
                log.warn("AI recommendation unavailable, falling back to general: {}", e.getMessage());
            }
        }
        for (ApiDtos.RecommendationResponse r : general) {
            merged.add(new ApiDtos.LocationRecommendationResponse(
                    r.id(), r.scene(), r.title(), r.description(), r.tags(),
                    r.score(), r.baseScore(), r.active(),
                    null, null, "general"));
        }
        return merged.stream().limit(15).toList();
    }

    @Transactional
    public void feedback(UserAccount user, Long itemId, ApiDtos.FeedbackRequest request) {
        RecommendationItem item = items.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "推荐项不存在"));
        FeedbackRating rating = EnumParser.rating(request.rating());
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setItem(item);
        feedback.setRating(rating);
        feedback.setComment(request.comment());
        feedbacks.save(feedback);

        int delta = switch (rating) {
            case LIKE -> 3;
            case NEUTRAL -> 1;
            case DISLIKE -> -3;
        };
        for (String tag : item.getTags()) {
            UserPreference preference = preferences.findByUserAndTag(user, tag).orElseGet(() -> {
                UserPreference created = new UserPreference();
                created.setUser(user);
                created.setTag(tag);
                return created;
            });
            preference.setWeight(Math.max(-30, Math.min(30, preference.getWeight() + delta)));
            preferences.save(preference);
        }
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.UserFeedbackResponse> myFeedback(UserAccount user) {
        return feedbacks.findByUserOrderByCreatedAtDesc(user).stream()
                .map(f -> new ApiDtos.UserFeedbackResponse(
                        f.getId(),
                        f.getItem().getTitle(),
                        f.getItem().getScene().name(),
                        f.getRating().name(),
                        f.getComment(),
                        f.getCreatedAt()))
                .toList();
    }

    public Map<String, Integer> getMergedScores(UserAccount user) {
        return mergedLatestScores(user);
    }

    private Map<String, Integer> mergedLatestScores(UserAccount user) {
        TestResult primary = results.findFirstByUserAndTypeOrderByCreatedAtDesc(user, TestType.PERSONALITY)
                .orElseThrow(() -> new BusinessException(400, "请先完成基础性格测试"));

        Map<String, Integer> mergedScores = new HashMap<>();
        for (PersonalityDimension dimension : PersonalityDimension.values()) {
            mergedScores.put(dimension.name(), 50);
        }
        mergedScores.putAll(primary.getScores());

        for (TestType type : TestType.values()) {
            if (type == TestType.PERSONALITY) {
                continue;
            }
            results.findFirstByUserAndTypeOrderByCreatedAtDesc(user, type)
                    .ifPresent(result -> result.getScores()
                            .forEach((dimension, score) -> mergedScores.merge(dimension, score, Math::max)));
        }
        return mergedScores;
    }
}
