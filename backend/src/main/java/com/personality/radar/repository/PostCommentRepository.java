package com.personality.radar.repository;

import com.personality.radar.domain.Post;
import com.personality.radar.domain.PostComment;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostAndActiveTrueOrderByCreatedAtAsc(Post post, Pageable pageable);
    List<PostComment> findByUserAndActiveTrueOrderByCreatedAtDesc(UserAccount user, Pageable pageable);
}
