package com.personality.radar.ai;

import java.util.Map;

/**
 * B → C 内部契约（入参）。
 * B 负责组装好这个上下文交给 C 的 {@link AiRecommendationProvider} 实现；
 * C 不需要关心用户怎么来的、画像怎么算的，只管根据这些信息产出推荐。
 *
 * @param userId        当前用户 id（仅用于日志/排错，C 不应据此做业务判断）
 * @param scene         场景，目前只有 "food"
 * @param profileScores 10 维画像分（键见 PersonalityDimension，例如 FOOD_ADVENTURE / FOOD_SOCIAL）
 * @param lat           纬度，可能为空（用户拒绝定位时）
 * @param lng           经度，可能为空
 * @param city          城市/商圈名，定位拿不到时由前端手选传入，可能为空
 * @param limit         期望返回的条数上限
 */
public record AiRecoContext(
        Long userId,
        String scene,
        Map<String, Integer> profileScores,
        Double lat,
        Double lng,
        String city,
        int limit) {
}