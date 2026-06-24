package com.personality.radar.service;

import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.*;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenMatchService {

    private final OpenMatchProfileRepository profiles;
    private final UserRepository users;
    private final TestResultRepository results;
    private final FriendshipRepository friendships;

    public OpenMatchService(OpenMatchProfileRepository profiles, UserRepository users,
                            TestResultRepository results, FriendshipRepository friendships) {
        this.profiles = profiles;
        this.users = users;
        this.results = results;
        this.friendships = friendships;
    }

    @Transactional
    public ApiDtos.OpenMatchStatusResponse toggle(UserAccount user) {
        OpenMatchProfile profile = profiles.findByUser(user)
                .orElseGet(() -> {
                    OpenMatchProfile p = new OpenMatchProfile();
                    p.setUser(user);
                    p.setEnabled(false);
                    return p;
                });

        if (profile.isEnabled()) {
            // 关闭匹配
            profile.setEnabled(false);
            profile.setPersonalityJson(null);
            profile.setUpdatedAt(Instant.now());
            profiles.save(profile);
            return new ApiDtos.OpenMatchStatusResponse(false, "匹配已关闭，你的数据已从匹配池中移除");
        } else {
            // 开启匹配：获取最新的10维人格分数
            TestResult latest = results.findFirstByUserAndTypeOrderByCreatedAtDesc(user, TestType.PERSONALITY)
                    .orElseThrow(() -> new BusinessException(400, "请先完成基础性格测试，再开启匹配"));
            Map<String, Integer> scores = latest.getScores();
            profile.setEnabled(true);
            profile.setPersonalityJson(toJson(scores));
            profile.setUpdatedAt(Instant.now());
            profiles.save(profile);
            return new ApiDtos.OpenMatchStatusResponse(true, "匹配已开启，你的10维人格分数已进入匹配池");
        }
    }

    @Transactional(readOnly = true)
    public ApiDtos.OpenMatchStatusResponse getStatus(UserAccount user) {
        OpenMatchProfile profile = profiles.findByUser(user).orElse(null);
        if (profile == null || !profile.isEnabled()) {
            return new ApiDtos.OpenMatchStatusResponse(false, "匹配未开启");
        }
        return new ApiDtos.OpenMatchStatusResponse(true, "匹配已开启");
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.OpenMatchRecommendationResponse> getRecommendations(UserAccount user) {
        OpenMatchProfile myProfile = profiles.findByUser(user)
                .orElseThrow(() -> new BusinessException(400, "请先开启开放匹配"));
        if (!myProfile.isEnabled()) {
            throw new BusinessException(400, "请先开启开放匹配，才能查看推荐");
        }

        Map<String, Integer> myScores = parseScores(myProfile.getPersonalityJson());

        // 获取所有已启用的匹配档案（排除自己）
        List<OpenMatchProfile> allProfiles = profiles.findByEnabledTrue();
        List<OpenMatchProfile> candidates = allProfiles.stream()
                .filter(p -> !p.getUser().getId().equals(user.getId()))
                .toList();

        if (candidates.isEmpty()) {
            return List.of();
        }

        // 获取当前用户的好友列表和拉黑列表（用于过滤）
        List<Friendship> myFriendships = friendships.findByUser(user);
        Set<Long> excludedUserIds = new HashSet<>();
        for (Friendship f : myFriendships) {
            if (f.getStatus() == Friendship.FriendshipStatus.ACTIVE
                    || f.getStatus() == Friendship.FriendshipStatus.BLOCKED) {
                excludedUserIds.add(f.getFriend().getId());
            }
        }

        List<ApiDtos.OpenMatchRecommendationResponse> recommendations = new ArrayList<>();

        for (OpenMatchProfile candidate : candidates) {
            Long candidateUserId = candidate.getUser().getId();
            if (excludedUserIds.contains(candidateUserId)) {
                continue;
            }

            Map<String, Integer> candidateScores = parseScores(candidate.getPersonalityJson());
            double score = MatchEngine.compatibilityScore(myScores, candidateScores);

            if (score >= 80.0) {
                List<String> topDimensions = findTopMatchingDimensions(myScores, candidateScores, 3);
                recommendations.add(new ApiDtos.OpenMatchRecommendationResponse(
                        DtoMapper.user(candidate.getUser()),
                        Math.round(score * 100.0) / 100.0,
                        topDimensions));
            }
        }

        recommendations.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (recommendations.size() > 20) {
            return recommendations.subList(0, 20);
        }
        return recommendations;
    }

    // ─── 内部工具方法 ──────────────────────────────────────────

    private String toJson(Map<String, Integer> scores) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseScores(String json) {
        Map<String, Integer> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        String content = json.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }
        if (content.isBlank()) return map;
        for (String part : content.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                try {
                    int value = Integer.parseInt(kv[1].trim());
                    map.put(key, value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }

    private List<String> findTopMatchingDimensions(Map<String, Integer> a, Map<String, Integer> b, int count) {
        List<String> dims = new ArrayList<>();
        for (PersonalityDimension d : PersonalityDimension.values()) {
            dims.add(d.name());
        }
        dims.sort((d1, d2) -> {
            int diff1 = Math.abs(a.getOrDefault(d1, 50) - b.getOrDefault(d1, 50));
            int diff2 = Math.abs(a.getOrDefault(d2, 50) - b.getOrDefault(d2, 50));
            return Integer.compare(diff1, diff2);
        });
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, dims.size()); i++) {
            String dim = dims.get(i);
            result.add(PersonalityDimension.valueOf(dim).label());
        }
        return result;
    }
}
