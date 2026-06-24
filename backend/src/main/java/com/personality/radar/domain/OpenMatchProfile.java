package com.personality.radar.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = {
    @Index(name = "idx_omp_enabled", columnList = "enabled")
})
public class OpenMatchProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private UserAccount user;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(columnDefinition = "TEXT")
    private String personalityJson;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPersonalityJson() {
        return personalityJson;
    }

    public void setPersonalityJson(String personalityJson) {
        this.personalityJson = personalityJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
