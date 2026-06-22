package com.personality.radar.repository;

import com.personality.radar.domain.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByCityIdOrderByName(Long cityId);
}
