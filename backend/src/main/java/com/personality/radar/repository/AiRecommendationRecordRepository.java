package com.personality.radar.repository;

import com.personality.radar.domain.AiRecommendationRecord;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRecordRepository extends JpaRepository<AiRecommendationRecord, Long> {

    List<AiRecommendationRecord> findByUserOrderByCreatedAtDesc(UserAccount user);

    Optional<AiRecommendationRecord> findByIdAndUser(Long id, UserAccount user);
}