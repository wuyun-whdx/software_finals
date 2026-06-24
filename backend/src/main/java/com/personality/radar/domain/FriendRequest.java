package com.personality.radar.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = {
    @Index(name = "idx_fr_to_user_status", columnList = "to_user_id, status, created_at DESC"),
    @Index(name = "idx_fr_from_user_status", columnList = "from_user_id, status, created_at DESC")
})
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private UserAccount fromUser;

    @ManyToOne(optional = false)
    private UserAccount toUser;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(length = 50)
    private String message;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED, BLOCKED
    }

    public Long getId() {
        return id;
    }

    public UserAccount getFromUser() {
        return fromUser;
    }

    public void setFromUser(UserAccount fromUser) {
        this.fromUser = fromUser;
    }

    public UserAccount getToUser() {
        return toUser;
    }

    public void setToUser(UserAccount toUser) {
        this.toUser = toUser;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
