package com.personality.radar.repository;

import com.personality.radar.domain.Friendship;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserAndStatusOrderByUpdatedAtDesc(UserAccount user, Friendship.FriendshipStatus status);

    Optional<Friendship> findByUserAndFriend(UserAccount user, UserAccount friend);

    List<Friendship> findByUser(UserAccount user);

    boolean existsByUserAndFriendAndStatus(UserAccount user, UserAccount friend, Friendship.FriendshipStatus status);
}
