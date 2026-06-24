package com.personality.radar.controller;

import com.personality.radar.ai.client.DeepSeekClient;
import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.service.CurrentUserService;
import com.personality.radar.service.TestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tests")
public class TestController {
    private final CurrentUserService currentUser;
    private final TestService testService;

    public TestController(CurrentUserService currentUser, TestService testService) {
        this.currentUser = currentUser;
        this.testService = testService;
    }

    @PostMapping("/submit")
    public ApiResponse<ApiDtos.TestResultResponse> submit(@Valid @RequestBody ApiDtos.TestSubmitRequest request) {
        return ApiResponse.ok(testService.submit(currentUser.requireUser(), request));
    }

    @GetMapping("/history")
    public ApiResponse<List<ApiDtos.TestResultResponse>> history() {
        return ApiResponse.ok(testService.history(currentUser.requireUser()));
    }
}

