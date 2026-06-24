package com.personality.radar.service;

import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.*;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final int PAGE_SIZE = 50;

    private final ChatMessageRepository messages;
    private final FriendshipRepository friendships;
    private final UserRepository users;

    public ChatService(ChatMessageRepository messages, FriendshipRepository friendships,
                       UserRepository users) {
        this.messages = messages;
        this.friendships = friendships;
        this.users = users;
    }

    @Transactional
    public ApiDtos.ChatMessageResponse sendMessage(UserAccount sender, Long friendId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(400, "消息内容不能超过500字");
        }
        UserAccount receiver = users.findById(friendId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (!friendships.existsByUserAndFriendAndStatus(sender, receiver, Friendship.FriendshipStatus.ACTIVE)) {
            throw new BusinessException(403, "你们不是好友，无法发送消息");
        }
        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content.trim());
        msg.setRead(false);
        msg.setCreatedAt(Instant.now());
        messages.save(msg);
        return toResponse(msg);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ChatMessageResponse> getMessages(UserAccount user, Long friendId, int page) {
        UserAccount friend = users.findById(friendId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        List<ChatMessage> msgs = messages.findConversation(
                user, friend, PageRequest.of(page, PAGE_SIZE));
        List<ApiDtos.ChatMessageResponse> result = new ArrayList<>();
        for (ChatMessage m : msgs) {
            result.add(toResponse(m));
        }
        result.sort(Comparator.comparing(ApiDtos.ChatMessageResponse::createdAt));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ChatConversationResponse> getConversations(UserAccount user) {
        List<Friendship> friends = friendships.findByUserAndStatusOrderByUpdatedAtDesc(
                user, Friendship.FriendshipStatus.ACTIVE);

        List<ApiDtos.ChatConversationResponse> result = new ArrayList<>();
        for (Friendship f : friends) {
            UserAccount friend = f.getFriend();
            long unread = messages.countUnreadByReceiverAndSender(user, friend);
            List<ChatMessage> lastMsgs = messages.findConversation(
                    user, friend, PageRequest.of(0, 1));
            String lastMessage = "";
            Instant lastTime = f.getUpdatedAt();
            if (!lastMsgs.isEmpty()) {
                ChatMessage last = lastMsgs.get(0);
                lastMessage = last.getContent().length() > 20
                        ? last.getContent().substring(0, 20) + "..."
                        : last.getContent();
                lastTime = last.getCreatedAt();
            }
            result.add(new ApiDtos.ChatConversationResponse(
                    DtoMapper.user(friend), lastMessage, lastTime, unread));
        }
        result.sort((a, b) -> b.lastMessageTime().compareTo(a.lastMessageTime()));
        return result;
    }

    @Transactional
    public void markRead(UserAccount user, Long friendId) {
        UserAccount friend = users.findById(friendId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        List<ChatMessage> unread = messages.findUnreadMessages(friend, user);
        for (ChatMessage msg : unread) {
            msg.setRead(true);
        }
        messages.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserAccount user) {
        return messages.countUnreadByReceiver(user);
    }

    private ApiDtos.ChatMessageResponse toResponse(ChatMessage msg) {
        return new ApiDtos.ChatMessageResponse(
                msg.getId(),
                DtoMapper.user(msg.getSender()),
                msg.getContent(),
                msg.isRead(),
                msg.getCreatedAt());
    }
}
