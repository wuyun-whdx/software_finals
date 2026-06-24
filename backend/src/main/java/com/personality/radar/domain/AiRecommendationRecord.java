package com.personality.radar.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 每次 AI 推荐的落库记录。
 *
 * 为什么要单独建这张表：现有 Feedback 绑死了 RecommendationItem，而 AI 结果是临时生成、
 * 不在 recommendation_item 表里的。要让用户能对 AI 结果点赞/点踩，就需要先把这次结果存下来、
 * 拿到一个 recordId，反馈再挂到这个 recordId 上。同时这张表也是后续做效果分析/调 Prompt 的数据源。
 *
 * items 直接存 JSON 字符串（就是返回给前端的那批条目），简单够用。
 */
@Entity
@Table(name = "ai_recommendation_record", indexes = {
        @Index(name = "idx_ai_reco_user_created", columnList = "user_id, created_at DESC")
})
public class AiRecommendationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private UserAccount user;

    @Column(nullable = false, length = 24)
    private String scene;

    /** AI 或 RULE_FALLBACK */
    @Column(nullable = false, length = 24)
    private String source;

    @Column(length = 80)
    private String city;

    private Double lat;

    private Double lng;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String itemsJson;

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

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}