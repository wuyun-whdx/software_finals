package com.personality.radar.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 对某条 AI 推荐结果的反馈。和现有 Feedback 区分开：
 * 现有 Feedback 指向 RecommendationItem，这里指向 AiRecommendationRecord + 具体某条的名字。
 * 复用了 {@link FeedbackRating} 枚举，保持评价口径一致（LIKE/NEUTRAL/DISLIKE）。
 */
@Entity
@Table(name = "ai_recommendation_feedback", indexes = {
        @Index(name = "idx_ai_feedback_user_created", columnList = "user_id, created_at DESC")
})
public class AiRecommendationFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private UserAccount user;

    @ManyToOne(optional = false)
    private AiRecommendationRecord record;

    /** 针对记录里的哪一条（按餐厅名定位），可空表示对整次推荐的反馈。 */
    @Column(length = 120)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackRating rating;

    @Column(length = 100)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public AiRecommendationRecord getRecord() {
        return record;
    }

    public void setRecord(AiRecommendationRecord record) {
        this.record = record;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public FeedbackRating getRating() {
        return rating;
    }

    public void setRating(FeedbackRating rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}