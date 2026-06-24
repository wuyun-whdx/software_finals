package com.personality.radar.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 推荐相关配置，全部带默认值，所以不改 application.yml 也能跑。
 * 想调参时在 application.yml 里加 app.ai.* 即可（见随附的 application-ai-snippet.yml）。
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 选用哪个 provider：mock（默认）或 real（C 上传后切到这个）。 */
    private String provider = "mock";

    /** 单次调用 provider 的超时（毫秒），超时则降级到规则推荐。 */
    private long timeoutMs = 6000;

    /** 结果缓存 TTL（秒），实时性要求决定，默认 10 分钟。 */
    private long cacheTtlSeconds = 600;

    /** 每个用户每分钟最多真正触发几次 provider 调用（控成本），缓存命中不计数。 */
    private int rateLimitPerMinute = 5;

    /** 默认返回条数。 */
    private int defaultLimit = 5;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }
}