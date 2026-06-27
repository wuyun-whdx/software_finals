package com.personality.radar.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DeepSeekClient {

    @Value("${app.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${app.ai.deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate;

    public DeepSeekClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(25000);
        this.restTemplate = new RestTemplate(factory);
    }

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public String chat(String prompt) {

        try {

            String url = baseUrl + "/chat/completions";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON);

            headers.setBearerAuth(apiKey);

            Map<String, Object> body =
                    Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of(
                                            "role", "system",
                                            "content", "你只输出一个合法 JSON 对象，不输出 Markdown 或解释。"
                                    ),
                                    Map.of(
                                            "role", "user",
                                            "content", prompt
                                    )
                            ),
                            "temperature", 0.2,
                            "max_tokens", 1200,
                            "response_format", Map.of("type", "json_object")
                    );

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            JsonNode root =
                    objectMapper.readTree(
                            response.getBody());

            return root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}