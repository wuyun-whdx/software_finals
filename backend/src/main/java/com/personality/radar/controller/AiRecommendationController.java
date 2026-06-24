package com.personality.radar.controller;

import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.AiDtos;
import com.personality.radar.service.AiRecommendationService;
import com.personality.radar.service.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 实时餐饮推荐接口。路径在 /api/recommendations/ai 下，
 * 已被 SecurityConfig 的 anyRequest().authenticated() 覆盖，无需改安全配置。
 *
 * 这里就是 A↔B 契约的落点：A 调这三个接口。
 */
@RestController
@RequestMapping("/api/recommendations/ai")
public class AiRecommendationController {
    private final CurrentUserService currentUser;
    private final AiRecommendationService aiService;

    public AiRecommendationController(CurrentUserService currentUser, AiRecommendationService aiService) {
        this.currentUser = currentUser;
        this.aiService = aiService;
    }

    /**
     * GET /api/recommendations/ai?scene=food&lat=..&lng=..&city=..&limit=5
     * lat/lng/city 都是可选：拿不到定位时前端传 city，全都没有也能返回（noloc）。
     */
    @GetMapping
    public ApiResponse<AiDtos.AiRecommendationResponse> recommend(
            @RequestParam(required = false, defaultValue = "food") String scene,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(aiService.recommend(currentUser.requireUser(), scene, lat, lng, city, limit));
    }

    /** POST /api/recommendations/ai/{recordId}/feedback */
    @PostMapping("/{recordId}/feedback")
    public ApiResponse<Void> feedback(
            @PathVariable Long recordId,
            @Valid @RequestBody AiDtos.AiFeedbackRequest request) {
        aiService.feedback(currentUser.requireUser(), recordId, request);
        return ApiResponse.ok();
    }

    /** GET /api/recommendations/ai/history */
    @GetMapping("/history")
    public ApiResponse<List<AiDtos.AiRecordSummaryResponse>> history() {
        return ApiResponse.ok(aiService.history(currentUser.requireUser()));
    }
}