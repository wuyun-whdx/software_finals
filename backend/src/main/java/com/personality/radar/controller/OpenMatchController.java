package com.personality.radar.controller;

import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.service.CurrentUserService;
import com.personality.radar.service.OpenMatchService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match/open")
public class OpenMatchController {

    private final CurrentUserService currentUser;
    private final OpenMatchService openMatchService;

    public OpenMatchController(CurrentUserService currentUser, OpenMatchService openMatchService) {
        this.currentUser = currentUser;
        this.openMatchService = openMatchService;
    }

    @PostMapping("/toggle")
    public ApiResponse<ApiDtos.OpenMatchStatusResponse> toggle() {
        return ApiResponse.ok(openMatchService.toggle(currentUser.requireUser()));
    }

    @GetMapping("/status")
    public ApiResponse<ApiDtos.OpenMatchStatusResponse> status() {
        return ApiResponse.ok(openMatchService.getStatus(currentUser.requireUser()));
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<ApiDtos.OpenMatchRecommendationResponse>> recommendations() {
        return ApiResponse.ok(openMatchService.getRecommendations(currentUser.requireUser()));
    }
}
