package com.personality.radar.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.ai.dto.RestaurantCandidate;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MapClient {

    private static final String FOOD_KEYWORD = "\u9910\u5385";
    private static final List<String> TRAVEL_KEYWORDS = List.of(
            "\u666f\u70b9",
            "\u535a\u7269\u9986",
            "\u516c\u56ed",
            "\u5386\u53f2\u8857\u533a",
            "\u53e4\u9547",
            "\u6b65\u884c\u8857");

    @Value("${ai.amap.key}")
    private String amapKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public double[] geocodeCity(String city) {
        try {
            URI districtUri = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/config/district")
                    .queryParam("key", amapKey)
                    .queryParam("keywords", city)
                    .queryParam("subdistrict", 0)
                    .queryParam("extensions", "base")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            String districtJson = restTemplate.getForObject(districtUri, String.class);
            JsonNode districtRoot = objectMapper.readTree(districtJson);
            JsonNode districts = districtRoot.get("districts");
            if (districts != null && districts.isArray() && !districts.isEmpty()) {
                String center = text(districts.get(0), "center");
                if (center != null && !center.isBlank()) {
                    return parseLocation(center);
                }
            }

            URI geocodeUri = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                    .queryParam("key", amapKey)
                    .queryParam("address", city)
                    .queryParam("city", city)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            String geocodeJson = restTemplate.getForObject(geocodeUri, String.class);
            JsonNode geocodeRoot = objectMapper.readTree(geocodeJson);
            JsonNode geocodes = geocodeRoot.get("geocodes");
            if (geocodes != null && geocodes.isArray() && !geocodes.isEmpty()) {
                String location = text(geocodes.get(0), "location");
                if (location != null && !location.isBlank()) {
                    return parseLocation(location);
                }
            }
            throw new IllegalStateException("AMap did not return city coordinates: " + city);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<RestaurantCandidate> nearbyRestaurants(double latitude, double longitude) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://restapi.amap.com/v5/place/around")
                    .queryParam("key", amapKey)
                    .queryParam("location", longitude + "," + latitude)
                    .queryParam("keywords", FOOD_KEYWORD)
                    .queryParam("radius", 5000)
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            return parsePois(restTemplate.getForObject(uri, String.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<RestaurantCandidate> travelPois(String city, int limit) {
        try {
            Map<String, RestaurantCandidate> merged = new LinkedHashMap<>();
            int pageSize = Math.min(Math.max(limit, 10), 20);
            for (String keyword : TRAVEL_KEYWORDS) {
                URI uri = UriComponentsBuilder
                        .fromHttpUrl("https://restapi.amap.com/v5/place/text")
                        .queryParam("key", amapKey)
                        .queryParam("keywords", keyword)
                        .queryParam("region", city)
                        .queryParam("city_limit", true)
                        .queryParam("page_size", pageSize)
                        .queryParam("page_num", 1)
                        .queryParam("show_fields", "business")
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUri();
                for (RestaurantCandidate candidate : parsePois(restTemplate.getForObject(uri, String.class))) {
                    merged.putIfAbsent(candidate.getName(), candidate);
                }
                if (merged.size() >= limit) {
                    break;
                }
            }
            return new ArrayList<>(merged.values());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<RestaurantCandidate> parsePois(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode pois = root.get("pois");
        Map<String, RestaurantCandidate> unique = new LinkedHashMap<>();
        if (pois == null || !pois.isArray()) {
            return List.of();
        }
        for (JsonNode poi : pois) {
            RestaurantCandidate item = new RestaurantCandidate();
            item.setName(text(poi, "name"));
            item.setCategory(text(poi, "type"));
            item.setAddress(text(poi, "address"));
            item.setDistance(poi.has("distance") ? poi.get("distance").asDouble() : null);
            item.setRating(doubleValue(poi, "rating"));
            item.setPriceLevel(priceValue(poi));
            item.setLocation(text(poi, "location"));
            if (item.getName() != null && !item.getName().isBlank()) {
                unique.putIfAbsent(item.getName(), item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private double[] parseLocation(String location) {
        String[] parts = location.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid AMap location: " + location);
        }
        double longitude = Double.parseDouble(parts[0].trim());
        double latitude = Double.parseDouble(parts[1].trim());
        return new double[] { latitude, longitude };
    }

    private Double doubleValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        String text = value.asText();
        if (text == null || text.isBlank() || "[]".equals(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String priceValue(JsonNode node) {
        String cost = text(node, "cost");
        if (cost == null || cost.isBlank() || "[]".equals(cost)) {
            return null;
        }
        return "\u4eba\u5747" + cost + "\u5143";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return "[]".equals(text) ? null : text;
    }
}