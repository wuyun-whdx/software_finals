package com.personality.radar.controller;

import com.personality.radar.ai.dto.CityCandidate;
import com.personality.radar.ai.service.CityRecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/cities")
public class CityController {

    private final CityRecommendationService service;

    public CityController(CityRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recommend")
    public List<CityCandidate> recommend(
            @RequestParam double openness,
            @RequestParam double conscientiousness,
            @RequestParam double extraversion,
            @RequestParam double agreeableness,
            @RequestParam double emotionalStability,
            @RequestParam double foodExplore,
            @RequestParam double foodSocial,
            @RequestParam double travelExplore,
            @RequestParam double travelPlan,
            @RequestParam double socialEnergy
    ) {
        return service.recommend(
                openness,
                conscientiousness,
                extraversion,
                agreeableness,
                emotionalStability,
                foodExplore,
                foodSocial,
                travelExplore,
                travelPlan,
                socialEnergy
        );
    }
}