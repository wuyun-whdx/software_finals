package com.personality.radar.ai;

import com.personality.radar.domain.PersonalityDimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * C 还没上传时的替身实现。
 *
 * 默认生效（app.ai.provider 缺省 = mock）。等 C 上传真实实现后，在配置里设置
 * app.ai.provider=real，C 的实现类用 @ConditionalOnProperty(..., havingValue="real")
 * 接管，本类自动失效。
 *
 * 它返回一批写死的样例餐厅，但会根据画像里的 FOOD_ADVENTURE / FOOD_SOCIAL 微调匹配度，
 * 让前端联调时看到的数据是「像被个性化过的」，便于 A 调样式、调交互、调反馈。
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiRecommendationProvider implements AiRecommendationProvider {

    private record Sample(String name, String category, String address,
                          double distance, double rating, String price,
                          int base, String adventurousReason, String safeReason,
                          List<String> tags, String mapUrl) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("巷口创作居酒屋", "日式·创意小料理", "示例市中央区 1-2-3", 320, 4.6, "¥120/人",
                    78, "你饮食探索分偏高，这家每周换菜单、主打实验性创意菜，正合你想尝新的胃口。",
                    "招牌菜稳定耐吃，环境安静，适合不想踩雷时的稳妥之选。",
                    List.of("创意菜", "适合尝鲜", "氛围安静"), "https://maps.example.com/p/1"),
            new Sample("老街牛肉拉面", "日式拉面", "示例市西区 5-6", 540, 4.4, "¥55/人",
                    70, "经典不踩雷，汤底浓厚份量足，探索之余也需要一家随时能回去的店。",
                    "你偏好熟悉稳定的口味，这家二十年老店出品稳定，是你的安全牌。",
                    List.of("高性价比", "老店", "快"), "https://maps.example.com/p/2"),
            new Sample("聚场·围炉烧烤", "烧烤·适合聚餐", "示例市东区 9-10", 760, 4.5, "¥95/人",
                    72, "你饮食社交分高，这家大桌围炉、适合一群人热闹分享，气氛拉满。",
                    "也可以小桌安静吃，分量灵活，人少人多都不尴尬。",
                    List.of("适合聚会", "可分享", "热闹"), "https://maps.example.com/p/3"),
            new Sample("一人食定食屋", "和风定食", "示例市中央区 2-4", 210, 4.3, "¥48/人",
                    66, "上新频率一般，但口味扎实，适合不想折腾时随手解决一餐。",
                    "你更享受安静独自用餐，这家有大量单人座，出餐快、不被打扰。",
                    List.of("一人食", "快", "安静"), "https://maps.example.com/p/4"),
            new Sample("发酵实验室·小酒馆", "融合·发酵料理", "示例市北区 11-1", 980, 4.7, "¥160/人",
                    80, "小众发酵料理 + 自然酒搭配，几乎天天有新东西，探索型选手的乐园。",
                    "偏实验性、价格略高，若你更想要确定性，可优先考虑前面几家。",
                    List.of("小众", "适合尝鲜", "有酒水"), "https://maps.example.com/p/5"));

    @Override
    public List<AiRecoItem> recommend(AiRecoContext context) {
        Map<String, Integer> scores = context.profileScores();
        int adventure = scores.getOrDefault(PersonalityDimension.FOOD_ADVENTURE.name(), 50);
        int social = scores.getOrDefault(PersonalityDimension.FOOD_SOCIAL.name(), 50);
        boolean adventurous = adventure >= 55;
        boolean sociable = social >= 55;

        List<AiRecoItem> items = new ArrayList<>();
        for (Sample s : SAMPLES) {
            int score = s.base();
            // 简单地根据画像微调匹配度，纯粹是为了让 mock 数据看起来「被个性化过」。
            if (adventurous && s.tags().contains("适合尝鲜")) {
                score += 12;
            }
            if (!adventurous && s.tags().contains("老店")) {
                score += 8;
            }
            if (sociable && s.tags().contains("适合聚会")) {
                score += 10;
            }
            if (!sociable && s.tags().contains("一人食")) {
                score += 8;
            }
            score = Math.max(0, Math.min(100, score));
            String reason = adventurous ? s.adventurousReason() : s.safeReason();
            items.add(new AiRecoItem(
                    s.name(), s.category(), s.address(), s.distance(), s.rating(), s.price(),
                    score, reason, s.tags(), s.mapUrl(), "mock-" + s.name().hashCode(),
                    null, null, null, null, null));
        }
        items.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));
        int limit = Math.max(1, context.limit());
        return items.size() > limit ? items.subList(0, limit) : items;
    }

    @Override
    public String name() {
        return "mock";
    }
}