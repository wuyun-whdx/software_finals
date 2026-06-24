package com.personality.radar.ai;

import java.util.List;

/**
 * 【B 和 C 之间唯一的契约】
 *
 * B 的编排服务只依赖这个接口，永远不碰 C 的具体实现。
 * - C 没上传时：由 {@link MockAiRecommendationProvider} 顶替，B 的全链路照常跑通。
 * - C 上传后：C 写一个实现类（地图 POI + LLM），把配置 app.ai.provider 改成 real 即可切换，
 *   B 的业务代码一行都不用动。
 *
 * C 实现时唯一要做的事：把 {@link AiRecoContext} 变成排好序的 {@link AiRecoItem} 列表。
 * 失败就抛异常（B 会捕获并降级到规则推荐），不要自己吞掉返回空列表。
 */
public interface AiRecommendationProvider {

    /**
     * 根据画像 + 位置产出排序后的推荐列表。
     * 实现内部允许耗时（调地图、调 LLM），B 会用超时包住这次调用。
     */
    List<AiRecoItem> recommend(AiRecoContext context);

    /**
     * 实现名，仅用于日志与排错，例如 "mock" / "amap+qwen"。
     */
    String name();
}