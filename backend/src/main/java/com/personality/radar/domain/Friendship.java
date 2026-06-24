package com.personality.radar.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}),
    indexes = {
        @Index(name = "idx_friendship_user_status", columnList = "user_id, status"),
        @Index(name = "idx_friendship_friend_status", columnList = "friend_id, status")
    }
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private UserAccount user;

    @ManyToOne(optional = false)
    private UserAccount friend;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status = FriendshipStatus.ACTIVE;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum FriendshipStatus {
        ACTIVE, BLOCKED, DELETED
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public UserAccount getFriend() {
        return friend;
    }

    public void setFriend(UserAccount friend) {
        this.friend = friend;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
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
