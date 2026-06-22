package com.personality.radar.controller;

import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.service.CurrentUserService;
import com.personality.radar.service.RecommendationService;
import com.personality.radar.service.RegionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecommendationController {
    private final CurrentUserService currentUser;
    private final RecommendationService recommendationService;
    private final RegionService regionService;

    public RecommendationController(CurrentUserService currentUser,
                                    RecommendationService recommendationService,
                                    RegionService regionService) {
        this.currentUser = currentUser;
        this.recommendationService = recommendationService;
        this.regionService = regionService;
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<ApiDtos.RecommendationResponse>> list(
            @RequestParam String scene,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district) {
        return ApiResponse.ok(recommendationService.recommendWithRegion(
                currentUser.requireUser(), scene, province, city, district));
    }

    @PostMapping("/recommendations/{id}/feedback")
    public ApiResponse<Void> feedback(@PathVariable Long id, @Valid @RequestBody ApiDtos.FeedbackRequest request) {
        recommendationService.feedback(currentUser.requireUser(), id, request);
        return ApiResponse.ok();
    }

    @GetMapping("/recommendations/feedback/me")
    public ApiResponse<List<ApiDtos.UserFeedbackResponse>> myFeedback() {
        return ApiResponse.ok(recommendationService.myFeedback(currentUser.requireUser()));
    }

    // === Region endpoints ===
    @GetMapping("/regions/provinces")
    public ApiResponse<List<ApiDtos.SimpleRegion>> provinces() {
        return ApiResponse.ok(regionService.listProvinces());
    }

    @GetMapping("/regions/cities")
    public ApiResponse<List<ApiDtos.SimpleRegion>> cities(@RequestParam Long provinceId) {
        return ApiResponse.ok(regionService.listCities(provinceId));
    }

    @GetMapping("/regions/districts")
    public ApiResponse<List<ApiDtos.SimpleRegion>> districts(@RequestParam Long cityId) {
        return ApiResponse.ok(regionService.listDistricts(cityId));
    }

    @GetMapping("/user/region")
    public ApiResponse<ApiDtos.RegionResponse> getMyRegion() {
        return ApiResponse.ok(regionService.getCurrent(currentUser.requireUser()));
    }

    @PostMapping("/user/region")
    public ApiResponse<ApiDtos.RegionResponse> saveMyRegion(@RequestBody ApiDtos.RegionRequest request) {
        return ApiResponse.ok(regionService.save(
                currentUser.requireUser(), request.province(), request.city(), request.district()));
    }

    @GetMapping("/user/region/history")
    public ApiResponse<List<ApiDtos.RegionRecord>> regionHistory() {
        return ApiResponse.ok(regionService.history(currentUser.requireUser()));
    }
}
