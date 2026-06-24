package com.personality.radar.repository;

import com.personality.radar.domain.FriendInvite;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendInviteRepository extends JpaRepository<FriendInvite, Long> {

    Optional<FriendInvite> findByCode(String code);

    List<FriendInvite> findByOwnerOrderByCreatedAtDesc(UserAccount owner);

    Optional<FriendInvite> findByOwnerAndStatus(UserAccount owner, FriendInvite.InviteStatus status);
}
