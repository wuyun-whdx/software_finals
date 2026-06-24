package com.personality.radar.service;

import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.PersonalityDimension;
import com.personality.radar.domain.TestType;
import com.personality.radar.domain.UserAccount;
import com.personality.radar.repository.TestResultRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 组装用户的 10 维画像分，供 AI 推荐使用。
 *
 * 复刻了 ReportService 里跨四类测试取最新分、按维度取较大值的合并逻辑，
 * 但「不」走 ReportService，因为后者每次调用都会落一条 ReportSnapshot 快照；
 * AI 推荐可能高频调用，不该产生这种副作用。这里只读、不写。
 */
@Service
public class AiProfileAssembler {
    private final TestResultRepository results;

    public AiProfileAssembler(TestResultRepository results) {
        this.results = results;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> assemble(UserAccount user) {
        Map<String, Integer> merged = new HashMap<>();
        for (PersonalityDimension dim : PersonalityDimension.values()) {
            merged.put(dim.name(), 50);
        }
        boolean hasAnyResult = false;
        for (TestType type : TestType.values()) {
            var latest = results.findFirstByUserAndTypeOrderByCreatedAtDesc(user, type);
            if (latest.isPresent()) {
                hasAnyResult = true;
                latest.get().getScores().forEach((dim, score) -> merged.merge(dim, score, Math::max));
            }
        }
        if (!hasAnyResult) {
            throw new BusinessException(400, "请先完成测试后再获取 AI 推荐");
        }
        return merged;
    }
}