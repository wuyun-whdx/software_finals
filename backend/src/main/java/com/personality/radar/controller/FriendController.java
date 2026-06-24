package com.personality.radar.controller;

import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.service.CurrentUserService;
import com.personality.radar.service.FriendService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final CurrentUserService currentUser;
    private final FriendService friendService;

    public FriendController(CurrentUserService currentUser, FriendService friendService) {
        this.currentUser = currentUser;
        this.friendService = friendService;
    }

    @PostMapping("/invite")
    public ApiResponse<ApiDtos.FriendInviteResponse> createInvite() {
        return ApiResponse.ok(friendService.createFriendInvite(currentUser.requireUser()));
    }

    @GetMapping("/invites")
    public ApiResponse<List<ApiDtos.FriendInviteResponse>> listInvites() {
        return ApiResponse.ok(friendService.listFriendInvites(currentUser.requireUser()));
    }

    @PostMapping("/by-invite")
    public ApiResponse<ApiDtos.FriendInviteResponse> addByInvite(
            @Valid @RequestBody ApiDtos.FriendByInviteRequest request) {
        return ApiResponse.ok(friendService.addByInvite(currentUser.requireUser(), request.inviteCode()));
    }

    @PostMapping("/request")
    public ApiResponse<ApiDtos.FriendRequestResponse> sendRequest(
            @Valid @RequestBody ApiDtos.FriendRequestSend request) {
        return ApiResponse.ok(friendService.sendRequest(currentUser.requireUser(), request));
    }

    @GetMapping("/requests")
    public ApiResponse<List<ApiDtos.FriendRequestResponse>> listRequests() {
        return ApiResponse.ok(friendService.listRequests(currentUser.requireUser()));
    }

    @PutMapping("/requests/{id}/accept")
    public ApiResponse<ApiDtos.FriendRequestResponse> acceptRequest(@PathVariable Long id) {
        return ApiResponse.ok(friendService.acceptRequest(currentUser.requireUser(), id));
    }

    @PutMapping("/requests/{id}/reject")
    public ApiResponse<ApiDtos.FriendRequestResponse> rejectRequest(@PathVariable Long id) {
        return ApiResponse.ok(friendService.rejectRequest(currentUser.requireUser(), id));
    }

    @PutMapping("/requests/{id}/block")
    public ApiResponse<ApiDtos.FriendRequestResponse> blockRequest(@PathVariable Long id) {
        return ApiResponse.ok(friendService.blockRequest(currentUser.requireUser(), id));
    }

    @DeleteMapping("/requests/{id}")
    public ApiResponse<Void> cancelRequest(@PathVariable Long id) {
        friendService.cancelRequest(currentUser.requireUser(), id);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<ApiDtos.UserProfileResponse>> listFriends() {
        return ApiResponse.ok(friendService.listFriends(currentUser.requireUser()));
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId) {
        friendService.deleteFriend(currentUser.requireUser(), friendId);
        return ApiResponse.ok();
    }

    @PutMapping("/{friendId}/block")
    public ApiResponse<Void> blockFriend(@PathVariable Long friendId) {
        friendService.blockFriend(currentUser.requireUser(), friendId);
        return ApiResponse.ok();
    }

    @GetMapping("/blocked")
    public ApiResponse<List<ApiDtos.UserProfileResponse>> listBlocked() {
        return ApiResponse.ok(friendService.listBlocked(currentUser.requireUser()));
    }

    @PutMapping("/{friendId}/unblock")
    public ApiResponse<Void> unblockFriend(@PathVariable Long friendId) {
        friendService.unblockFriend(currentUser.requireUser(), friendId);
        return ApiResponse.ok();
    }

    @GetMapping("/search")
    public ApiResponse<ApiDtos.UserProfileResponse> searchByPhone(@RequestParam String phone) {
        return ApiResponse.ok(friendService.searchByPhone(currentUser.requireUser(), phone));
    }
}
