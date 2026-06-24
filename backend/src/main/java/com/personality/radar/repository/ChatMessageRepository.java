package com.personality.radar.repository;

import com.personality.radar.domain.ChatMessage;
import com.personality.radar.domain.UserAccount;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (m.sender = :user AND m.receiver = :friend)
           OR (m.sender = :friend AND m.receiver = :user)
        ORDER BY m.createdAt DESC
        """)
    List<ChatMessage> findConversation(@Param("user") UserAccount user,
                                       @Param("friend") UserAccount friend,
                                       Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiver = :receiver AND m.read = false")
    long countUnreadByReceiver(@Param("receiver") UserAccount receiver);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiver = :receiver AND m.sender = :sender AND m.read = false")
    long countUnreadByReceiverAndSender(@Param("receiver") UserAccount receiver,
                                         @Param("sender") UserAccount sender);

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.sender = :sender AND m.receiver = :receiver AND m.read = false
        """)
    List<ChatMessage> findUnreadMessages(@Param("sender") UserAccount sender,
                                          @Param("receiver") UserAccount receiver);

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (m.sender = :user OR m.receiver = :user)
          AND m.id IN (
            SELECT MAX(m2.id) FROM ChatMessage m2
            WHERE (m2.sender = :user OR m2.receiver = :user)
            GROUP BY CASE WHEN m2.sender = :user THEN m2.receiver ELSE m2.sender END
          )
        ORDER BY m.createdAt DESC
        """)
    List<ChatMessage> findLastMessagesByUser(@Param("user") UserAccount user);
}
