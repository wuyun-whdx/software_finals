package com.personality.radar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.deepseek")
public class AiProperties {
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 8;
    private int cacheTtlHours = 24;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getCacheTtlHours() { return cacheTtlHours; }
    public void setCacheTtlHours(int cacheTtlHours) { this.cacheTtlHours = cacheTtlHours; }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
