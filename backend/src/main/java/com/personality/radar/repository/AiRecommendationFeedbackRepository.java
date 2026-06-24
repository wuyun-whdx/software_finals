package com.personality.radar.repository;

import com.personality.radar.domain.AiRecommendationFeedback;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationFeedbackRepository extends JpaRepository<AiRecommendationFeedback, Long> {

    List<AiRecommendationFeedback> findByUserOrderByCreatedAtDesc(UserAccount user);
}