package com.personality.radar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class AiClient {
    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private final RestTemplate restTemplate;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    public String chat(String systemPrompt, String userMessage) {
        if (!properties.hasKey()) {
            log.warn("DeepSeek API key not configured, skipping AI call");
            return null;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return doChat(systemPrompt, userMessage);
            } catch (Exception e) {
                if (attempt == 0) {
                    log.warn("AI call attempt 1 failed, retrying: {}", e.getMessage());
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("AI call failed after retry: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    private String doChat(String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7,
                "max_tokens", 2048
        );

        String url = properties.getBaseUrl() + "/v1/chat/completions";
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        }
        return null;
    }
}
