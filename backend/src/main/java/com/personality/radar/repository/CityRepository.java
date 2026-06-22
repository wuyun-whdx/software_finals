package com.personality.radar.repository;

import com.personality.radar.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByProvinceIdOrderByName(Long provinceId);
}
