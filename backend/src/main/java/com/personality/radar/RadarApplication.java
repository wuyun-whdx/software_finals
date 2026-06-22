package com.personality.radar;

import com.personality.radar.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class RadarApplication {
    public static void main(String[] args) {
        SpringApplication.run(RadarApplication.class, args);
    }
}
