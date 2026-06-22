package com.personality.radar.repository;

import com.personality.radar.domain.UserAccount;
import com.personality.radar.domain.UserRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {
    Optional<UserRegion> findByUserAndIsCurrentTrue(UserAccount user);
    List<UserRegion> findByUserOrderByCreatedAtDesc(UserAccount user);
}
