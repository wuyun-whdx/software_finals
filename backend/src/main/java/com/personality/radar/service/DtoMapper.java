package com.personality.radar.service;

import com.personality.radar.domain.Question;
import com.personality.radar.domain.QuestionOption;
import com.personality.radar.domain.RecommendationItem;
import com.personality.radar.domain.RecommendationRule;
import com.personality.radar.domain.TestResult;
import com.personality.radar.domain.UserAccount;
import com.personality.radar.dto.ApiDtos;
import java.util.List;
import java.util.Map;

public final class DtoMapper {
    private DtoMapper() {
    }

    public static ApiDtos.UserProfileResponse user(UserAccount user) {
        return new ApiDtos.UserProfileResponse(
                user.getId(),
                user.getPhone(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.isActive());
    }

    public static ApiDtos.QuestionResponse question(Question question) {
        return new ApiDtos.QuestionResponse(
                question.getId(),
                question.getType().name().toLowerCase(),
                question.getContent(),
                question.isActive(),
                question.getOptions().stream().map(DtoMapper::option).toList());
    }

    public static ApiDtos.OptionResponse option(QuestionOption option) {
        return new ApiDtos.OptionResponse(
                option.getId(),
                option.getLabel(),
                option.getContent(),
                Map.copyOf(option.getWeights()));
    }

    public static ApiDtos.TestResultResponse testResult(TestResult result) {
        return new ApiDtos.TestResultResponse(
                result.getId(),
                result.getType().name().toLowerCase(),
                new java.util.HashMap<>(result.getScores()),
                result.getCreatedAt());
    }

    public static ApiDtos.RecommendationResponse recommendation(RecommendationItem item, int score) {
        return new ApiDtos.RecommendationResponse(
                item.getId(),
                item.getScene().name().toLowerCase(),
                item.getTitle(),
                item.getDescription(),
                List.copyOf(item.getTags()),
                score,
                item.getBaseScore(),
                item.isActive());
    }

    public static ApiDtos.LocationRecommendationResponse locationRecommendation(
            RecommendationItem item, int score, String address, String aiReason, String source) {
        return new ApiDtos.LocationRecommendationResponse(
                item.getId(),
                item.getScene().name().toLowerCase(),
                item.getTitle(),
                item.getDescription(),
                List.copyOf(item.getTags()),
                score,
                item.getBaseScore(),
                item.isActive(),
                address,
                aiReason,
                source);
    }

    public static ApiDtos.AdminUserResponse adminUser(UserAccount user) {
        return new ApiDtos.AdminUserResponse(
                user.getId(),
                user.getPhone(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.isActive(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    public static ApiDtos.RecommendationRuleResponse recommendationRule(RecommendationRule rule) {
        return new ApiDtos.RecommendationRuleResponse(
                rule.getId(),
                rule.getTag(),
                rule.getLabel(),
                rule.getWeight(),
                rule.isActive());
    }
}
