package com.personality.radar.repository;

import com.personality.radar.domain.OpenMatchProfile;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenMatchProfileRepository extends JpaRepository<OpenMatchProfile, Long> {

    Optional<OpenMatchProfile> findByUser(UserAccount user);

    List<OpenMatchProfile> findByEnabledTrue();
}
