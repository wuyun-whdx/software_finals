package com.personality.radar;

import com.personality.radar.ai.client.MapClient;
import com.personality.radar.ai.dto.RestaurantCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class MapClientTest {

    @Autowired
    private MapClient mapClient;

    @Test
    void testMapApi() {

        List<RestaurantCandidate> list =
                mapClient.nearbyRestaurants(
                        31.2304,
                        121.4737
                );

        list.forEach(System.out::println);
    }
}