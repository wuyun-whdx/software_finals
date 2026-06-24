package com.personality.radar.ai;

import java.util.List;

public record AiRecoItem(
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
        String externalId,
        String city,
        Integer durationDays,
        List<String> highlights,
        List<String> itinerary,
        List<String> tips) {
}