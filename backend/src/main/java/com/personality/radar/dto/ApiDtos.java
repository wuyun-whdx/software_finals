package com.personality.radar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^\\d{11}$") String phone,
            @NotBlank @Size(min = 6, max = 16) String password,
            @NotBlank @Size(max = 40) String displayName) {
    }

    public record LoginRequest(
            @NotBlank @Pattern(regexp = "^\\d{11}$") String phone,
            @NotBlank @Size(min = 6, max = 16) String password) {
    }

    public record UpdateUserRequest(
            @Size(max = 40) String displayName,
            @Size(max = 300) String avatarUrl) {
    }

    public record UserProfileResponse(Long id, String phone, String displayName, String avatarUrl, String role, boolean active) {
    }

    public record AuthResponse(String token, UserProfileResponse user) {
    }

    public record OptionResponse(Long id, String label, String content, Map<String, Integer> weights) {
    }

    public record QuestionResponse(Long id, String type, String content, boolean active, List<OptionResponse> options) {
    }

    public record AnswerInput(@NotNull Long questionId, @NotEmpty List<Long> optionIds) {
    }

    public record TestSubmitRequest(@NotBlank String type, @NotEmpty List<AnswerInput> answers) {
    }

    public record TestResultResponse(Long id, String type, Map<String, Integer> scores, Instant createdAt) {
    }

    public record RadarIndicator(String name, Integer max) {
    }

    public record ReportResponse(
            UserProfileResponse user,
            Map<String, Integer> scores,
            List<RadarIndicator> indicators,
            List<Integer> radarValues,
            List<String> interpretations,
            List<String> suggestions,
            Instant generatedAt) {
    }

    public record ReportSnapshotResponse(Long id, ReportResponse report, String summary, Instant createdAt) {
    }

    public record RecommendationResponse(
            Long id,
            String scene,
            String title,
            String description,
            List<String> tags,
            int score,
            Integer baseScore,
            boolean active) {
    }

    public record LocationRecommendationResponse(
            Long id,
            String scene,
            String title,
            String description,
            List<String> tags,
            int score,
            Integer baseScore,
            boolean active,
            String address,
            String aiReason,
            String source) {
    }

    public record FeedbackRequest(
            @NotBlank String rating,
            @Size(max = 100) String comment) {
    }

    public record MatchRequest(@NotBlank @Pattern(regexp = "^\\d{11}$") String friendPhone) {
    }

    public record MatchByInviteRequest(@NotBlank String inviteCode) {
    }

    public record MatchInviteResponse(
            String code,
            Instant createdAt,
            String status,
            Instant expiresAt) {
    }

    public record MatchResponse(
            Long id,
            UserProfileResponse owner,
            UserProfileResponse target,
            double score,
            String summary,
            List<String> advantages,
            List<String> warnings,
            List<String> advice,
            Map<String, Integer> ownerScores,
            Map<String, Integer> targetScores,
            Instant createdAt) {
    }

    public record ShareResponse(String token, String url, ReportResponse report) {
    }

    public record ShareLinkSummaryResponse(
            Long id,
            String token,
            String url,
            boolean active,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt) {
    }

    public record AdminOptionRequest(
            @NotBlank String label,
            @NotBlank String content,
            Map<String, Integer> weights) {
    }

    public record AdminQuestionRequest(
            @NotBlank String type,
            @NotBlank String content,
            Boolean active,
            @NotEmpty List<AdminOptionRequest> options) {
    }

    public record RecommendationItemRequest(
            @NotBlank String scene,
            @NotBlank String title,
            @NotBlank String description,
            List<String> tags,
            Integer baseScore,
            Boolean active) {
    }

    public record AdminStatsResponse(long users, long questions, long recommendations, long feedbacks, long matches) {
    }

    public record AdminLogResponse(Long id, String adminPhone, String action, String detail, Instant createdAt) {
    }

    public record AdminFeedbackResponse(
            Long id,
            String userPhone,
            String itemTitle,
            String scene,
            String rating,
            String comment,
            Instant createdAt) {
    }

    public record AdminDashboardResponse(
            AdminStatsResponse stats,
            Map<String, Long> testsByType,
            Map<String, Long> feedbackByRating,
            Map<String, Long> recommendationsByScene,
            long activeShares) {
    }

    public record AdminUserResponse(
            Long id,
            String phone,
            String displayName,
            String avatarUrl,
            String role,
            boolean active,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant lastLoginAt,
            Instant createdAt) {
    }

    public record AdminUserUpdateRequest(Boolean active, String role) {
    }

    public record RecommendationRuleRequest(
            @NotBlank String tag,
            @NotBlank String label,
            Integer weight,
            Boolean active) {
    }

    public record RecommendationRuleResponse(
            Long id,
            String tag,
            String label,
            int weight,
            boolean active) {
    }

    public record UserFeedbackResponse(
            Long id,
            String itemTitle,
            String scene,
            String rating,
            String comment,
            Instant createdAt) {
    }

    // === Community: Post DTOs ===
    public record CreatePostRequest(
            @NotBlank String content,
            @NotBlank String domainTag,
            List<String> styleTags) {
    }

    public record PostResponse(
            Long id,
            UserProfileResponse author,
            String content,
            String images,
            String domainTag,
            List<String> styleTags,
            Map<String, Integer> aiVector,
            String aiReviewStatus,
            int likeCount,
            int favoriteCount,
            int commentCount,
            int viewCount,
            int compatibility,
            boolean showCompatibility,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record PostListResponse(
            List<PostResponse> items,
            int total) {
    }

    public record CreateCommentRequest(
            @NotBlank String content) {
    }

    public record CommentResponse(
            Long id,
            String content,
            UserProfileResponse user,
            Instant createdAt) {
    }

    // === Region DTOs ===
    public record SimpleRegion(Long id, String name) {}

    public record RegionRequest(String province, String city, String district) {}

    public record RegionResponse(String province, String city, String district) {}

    public record RegionRecord(String province, String city, String district,
                               boolean isCurrent, Instant createdAt) {}
}
