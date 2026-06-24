package com.personality.radar.controller;

import com.personality.radar.ai.client.DeepSeekClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiTestController {

    private final DeepSeekClient deepSeekClient;

    public AiTestController(
            DeepSeekClient deepSeekClient) {

        this.deepSeekClient =
                deepSeekClient;
    }

    @GetMapping("/test/ai")
    public String testAi() {

        return deepSeekClient.chat(
                "请只回复一句话：你好，我是DeepSeek"
        );
    }
}