package com.personality.radar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AiDtos {
    private AiDtos() {
    }

    public record AiRecommendationItemResponse(
            String name,
            String category,
            String address,
            Double distanceMeters,
            Double rating,
            String priceLevel,
            int matchScore,
            String reason,
            List<String> tags,
            String mapUrl,
            String city,
            Integer durationDays,
            List<String> highlights,
            List<String> itinerary,
            List<String> tips) {
    }

    public record AiRecommendationResponse(
            Long recordId,
            String scene,
            String source,
            boolean degraded,
            String city,
            Instant generatedAt,
            List<AiRecommendationItemResponse> items) {
    }

    public record AiFeedbackRequest(
            @NotBlank String rating,
            @Size(max = 100) String comment,
            @Size(max = 120) String itemName) {
    }

    public record AiRecordSummaryResponse(
            Long recordId,
            String scene,
            String source,
            String city,
            int itemCount,
            Instant createdAt) {
    }
}