package com.personality.radar.repository;

import com.personality.radar.domain.FriendRequest;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByToUserAndStatusOrderByCreatedAtDesc(UserAccount toUser, FriendRequest.RequestStatus status);

    List<FriendRequest> findByFromUserAndStatusOrderByCreatedAtDesc(UserAccount fromUser, FriendRequest.RequestStatus status);

    Optional<FriendRequest> findByFromUserAndToUserAndStatus(UserAccount fromUser, UserAccount toUser, FriendRequest.RequestStatus status);

    boolean existsByFromUserAndToUserAndStatus(UserAccount fromUser, UserAccount toUser, FriendRequest.RequestStatus status);
}
