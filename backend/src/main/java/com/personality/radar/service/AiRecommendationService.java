package com.personality.radar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personality.radar.ai.AiProperties;
import com.personality.radar.ai.AiRecoContext;
import com.personality.radar.ai.AiRecoItem;
import com.personality.radar.ai.AiRecommendationProvider;
import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.AiRecommendationFeedback;
import com.personality.radar.domain.AiRecommendationRecord;
import com.personality.radar.domain.FeedbackRating;
import com.personality.radar.domain.SceneType;
import com.personality.radar.domain.UserAccount;
import com.personality.radar.dto.AiDtos;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.repository.AiRecommendationFeedbackRepository;
import com.personality.radar.repository.AiRecommendationRecordRepository;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B 的核心：AI 餐饮推荐编排。
 *
 * 一次 recommend 的完整链路：
 *   取画像 → 查缓存(命中即返回) → 限流 → 调 provider(带超时)
 *   → 成功则用 AI 结果 / 失败或超时则降级到规则推荐
 *   → 落库拿 recordId → 写缓存 → 返回。
 *
 * 唯一依赖 C 的地方是注入进来的 {@link AiRecommendationProvider}。C 没上传时这里注入的是
 * MockAiRecommendationProvider，全链路照样跑通；C 上传后切配置即可，本类不用改。
 */
@Service("bAiRecommendationService")
public class AiRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

    private final AiRecommendationProvider provider;
    private final AiProfileAssembler profileAssembler;
    private final RecommendationService recommendationService; // 降级用
    private final AiRecommendationRecordRepository records;
    private final AiRecommendationFeedbackRepository feedbacks;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final AiProperties props;

    // 专门用来给 provider 调用套超时，避免拖垮请求线程
    private final ExecutorService providerPool = Executors.newFixedThreadPool(4);

    public AiRecommendationService(
            AiRecommendationProvider provider,
            AiProfileAssembler profileAssembler,
            RecommendationService recommendationService,
            AiRecommendationRecordRepository records,
            AiRecommendationFeedbackRepository feedbacks,
            StringRedisTemplate redis,
            ObjectMapper mapper,
            AiProperties props) {
        this.provider = provider;
        this.profileAssembler = profileAssembler;
        this.recommendationService = recommendationService;
        this.records = records;
        this.feedbacks = feedbacks;
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
    }

    @PreDestroy
    void shutdown() {
        providerPool.shutdownNow();
    }

    @Transactional
    public AiDtos.AiRecommendationResponse recommend(
            UserAccount user, String scene, Double lat, Double lng, String city, Integer limit) {
        String sceneValue = (scene == null || scene.isBlank()) ? "food" : scene.trim().toLowerCase();
        int wanted = (limit == null || limit <= 0) ? props.getDefaultLimit() : Math.min(limit, 10);

        // 1) 缓存命中直接返回（缓存里已含 recordId，反馈仍然有效）
        String cacheKey = cacheKey(user.getId(), sceneValue, lat, lng, city);
        AiDtos.AiRecommendationResponse cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2) 画像（无任何测试结果会抛 400）
        Map<String, Integer> profile = profileAssembler.assemble(user);

        // 3) 限流（只在真正要调 provider 前计数，缓存命中不计）
        enforceRateLimit(user.getId());

        // 4) 调 provider，带超时；失败/超时降级
        AiRecoContext ctx = new AiRecoContext(user.getId(), sceneValue, profile, lat, lng, city, wanted);
        List<AiDtos.AiRecommendationItemResponse> items;
        String source;
        boolean degraded;
        try {
            List<AiRecoItem> raw = callWithTimeout(ctx);
            items = raw.stream().map(this::toItemResponse).toList();
            source = "AI";
            degraded = false;
            if (items.isEmpty()) {
                // provider 正常返回但没结果，也走降级，保证界面有内容
                throw new IllegalStateException("provider returned empty list");
            }
        } catch (Exception ex) {
            log.warn("AI provider [{}] 调用失败，降级到规则推荐: {}", provider.name(), ex.toString());
            items = fallbackFromRuleEngine(user, sceneValue, wanted);
            source = "RULE_FALLBACK";
            degraded = true;
        }

        // 5) 落库拿 recordId
        Long recordId = persist(user, sceneValue, source, city, lat, lng, items);

        // 6) 组装响应 + 写缓存
        AiDtos.AiRecommendationResponse response = new AiDtos.AiRecommendationResponse(
                recordId, sceneValue, source, degraded, city, Instant.now(), items);
        writeCache(cacheKey, response);
        return response;
    }

    @Transactional
    public List<ApiDtos.LocationRecommendationResponse> recommend(
            UserAccount user, SceneType scene, String province, String city, String district) {
        String sceneValue = scene == null ? "food" : scene.name().toLowerCase();
        AiDtos.AiRecommendationResponse response = recommend(
                user, sceneValue, null, null, city, props.getDefaultLimit());
        List<ApiDtos.LocationRecommendationResponse> mapped = new ArrayList<>();
        for (int i = 0; i < response.items().size(); i++) {
            AiDtos.AiRecommendationItemResponse item = response.items().get(i);
            int score = item.matchScore();
            mapped.add(new ApiDtos.LocationRecommendationResponse(
                    (long) -(i + 1),
                    sceneValue,
                    item.name(),
                    item.reason(),
                    item.tags() == null ? List.of() : item.tags(),
                    score,
                    null,
                    true,
                    item.address(),
                    item.reason(),
                    "ai"));
        }
        return mapped;
    }

    @Transactional
    public void feedback(UserAccount user, Long recordId, AiDtos.AiFeedbackRequest request) {
        AiRecommendationRecord record = records.findByIdAndUser(recordId, user)
                .orElseThrow(() -> new BusinessException(404, "推荐记录不存在"));
        FeedbackRating rating = EnumParser.rating(request.rating());
        AiRecommendationFeedback fb = new AiRecommendationFeedback();
        fb.setUser(user);
        fb.setRecord(record);
        fb.setItemName(request.itemName());
        fb.setRating(rating);
        fb.setComment(request.comment());
        feedbacks.save(fb);
    }

    @Transactional(readOnly = true)
    public List<AiDtos.AiRecordSummaryResponse> history(UserAccount user) {
        return records.findByUserOrderByCreatedAtDesc(user).stream()
                .map(r -> new AiDtos.AiRecordSummaryResponse(
                        r.getId(), r.getScene(), r.getSource(), r.getCity(),
                        countItems(r.getItemsJson()), r.getCreatedAt()))
                .toList();
    }

    // ---------- 内部工具 ----------

    private List<AiRecoItem> callWithTimeout(AiRecoContext ctx) throws Exception {
        Future<List<AiRecoItem>> future = providerPool.submit(() -> provider.recommend(ctx));
        try {
            return future.get(props.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /** 降级：复用现有规则推荐，把 RecommendationResponse 映射成 AI 响应的形状。 */
    private List<AiDtos.AiRecommendationItemResponse> fallbackFromRuleEngine(
            UserAccount user, String scene, int limit) {
        List<ApiDtos.RecommendationResponse> rule = recommendationService.recommend(user, scene);
        return rule.stream()
                .limit(limit)
                .map(r -> new AiDtos.AiRecommendationItemResponse(
                        r.title(), null, null, null, null, null,
                        r.score(), r.description(), r.tags(), null,
                        null, null, null, null, null))
                .toList();
    }

    private AiDtos.AiRecommendationItemResponse toItemResponse(AiRecoItem i) {
        return new AiDtos.AiRecommendationItemResponse(
                i.name(), i.category(), i.address(), i.distanceMeters(), i.rating(), i.priceLevel(),
                i.matchScore(), i.reason(), i.tags(), i.mapUrl(),
                i.city(), i.durationDays(), i.highlights(), i.itinerary(), i.tips());
    }

    private Long persist(UserAccount user, String scene, String source, String city,
                         Double lat, Double lng, List<AiDtos.AiRecommendationItemResponse> items) {
        AiRecommendationRecord record = new AiRecommendationRecord();
        record.setUser(user);
        record.setScene(scene);
        record.setSource(source);
        record.setCity(city);
        record.setLat(lat);
        record.setLng(lng);
        try {
            record.setItemsJson(mapper.writeValueAsString(items));
        } catch (Exception e) {
            throw new BusinessException(500, "推荐结果保存失败");
        }
        return records.save(record).getId();
    }

    private int countItems(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<Object>>() {}).size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void enforceRateLimit(Long userId) {
        long minute = System.currentTimeMillis() / 60_000;
        String key = "ai:rl:" + userId + ":" + minute;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, 70, TimeUnit.SECONDS);
            }
            if (count != null && count > props.getRateLimitPerMinute()) {
                throw new BusinessException(429, "AI 推荐调用过于频繁，请稍后再试");
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            // Redis 挂了不该阻断主流程，限流是尽力而为
            log.warn("限流计数失败，已跳过: {}", e.toString());
        }
    }

    private AiDtos.AiRecommendationResponse readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return mapper.readValue(json, AiDtos.AiRecommendationResponse.class);
        } catch (Exception e) {
            log.warn("读缓存失败，按未命中处理: {}", e.toString());
            return null;
        }
    }

    private void writeCache(String key, AiDtos.AiRecommendationResponse response) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(response),
                    props.getCacheTtlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写缓存失败，已忽略: {}", e.toString());
        }
    }

    /** 位置取到经纬度就按 ~100m 网格归一化做 key，否则退化到城市名。 */
    private String cacheKey(Long userId, String scene, Double lat, Double lng, String city) {
        String loc;
        if (lat != null && lng != null) {
            loc = String.format("%.3f,%.3f", lat, lng);
        } else if (city != null && !city.isBlank()) {
            loc = "city:" + city.trim();
        } else {
            loc = "noloc";
        }
        return "ai:reco:" + userId + ":" + scene + ":" + loc;
    }
}