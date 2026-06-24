package com.personality.radar.controller;

import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.service.CurrentUserService;
import com.personality.radar.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final CurrentUserService currentUser;
    private final ChatService chatService;

    public ChatController(CurrentUserService currentUser, ChatService chatService) {
        this.currentUser = currentUser;
        this.chatService = chatService;
    }

    @PostMapping("/messages/{friendId}")
    public ApiResponse<ApiDtos.ChatMessageResponse> sendMessage(
            @PathVariable Long friendId,
            @Valid @RequestBody ApiDtos.SendMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(currentUser.requireUser(), friendId, request.content()));
    }

    @GetMapping("/messages/{friendId}")
    public ApiResponse<List<ApiDtos.ChatMessageResponse>> getMessages(
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(chatService.getMessages(currentUser.requireUser(), friendId, page));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ApiDtos.ChatConversationResponse>> getConversations() {
        return ApiResponse.ok(chatService.getConversations(currentUser.requireUser()));
    }

    @PutMapping("/messages/{friendId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long friendId) {
        chatService.markRead(currentUser.requireUser(), friendId);
        return ApiResponse.ok();
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(chatService.getUnreadCount(currentUser.requireUser()));
    }
}
