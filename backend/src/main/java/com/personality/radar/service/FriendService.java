package com.personality.radar.service;

import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.*;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

    private static final int INVITE_EXPIRY_DAYS = 90;

    private final UserRepository users;
    private final FriendshipRepository friendships;
    private final FriendRequestRepository requests;
    private final FriendInviteRepository friendInvites;

    public FriendService(UserRepository users, FriendshipRepository friendships,
                         FriendRequestRepository requests, FriendInviteRepository friendInvites) {
        this.users = users;
        this.friendships = friendships;
        this.requests = requests;
        this.friendInvites = friendInvites;
    }

    // ─── 好友邀请码 ────────────────────────────────────────────

    @Transactional
    public ApiDtos.FriendInviteResponse createFriendInvite(UserAccount owner) {
        friendInvites.findByOwnerAndStatus(owner, FriendInvite.InviteStatus.ACTIVE)
                .ifPresent(invite -> {
                    invite.setStatus(FriendInvite.InviteStatus.REVOKED);
                    friendInvites.save(invite);
                });
        FriendInvite invite = new FriendInvite();
        invite.setCode(FriendInvite.generateCode());
        invite.setOwner(owner);
        invite.setStatus(FriendInvite.InviteStatus.ACTIVE);
        invite.setCreatedAt(Instant.now());
        invite.setExpiresAt(Instant.now().plus(INVITE_EXPIRY_DAYS, ChronoUnit.DAYS));
        friendInvites.save(invite);
        return new ApiDtos.FriendInviteResponse(
                invite.getCode(), invite.getCreatedAt(),
                invite.getStatus().name(), invite.getExpiresAt());
    }

    @Transactional
    public ApiDtos.FriendInviteResponse addByInvite(UserAccount user, String inviteCode) {
        FriendInvite invite = friendInvites.findByCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(404, "邀请码错误，请确认后重新输入。"));
        if (invite.getStatus() != FriendInvite.InviteStatus.ACTIVE) {
            throw new BusinessException(400, "该邀请码已失效（已使用或已撤销）。");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            invite.setStatus(FriendInvite.InviteStatus.REVOKED);
            friendInvites.save(invite);
            throw new BusinessException(400, "该邀请码已过期。");
        }
        UserAccount inviter = invite.getOwner();
        if (inviter.getId().equals(user.getId())) {
            throw new BusinessException(400, "不能添加自己为好友");
        }
        if (friendships.existsByUserAndFriendAndStatus(user, inviter, Friendship.FriendshipStatus.ACTIVE)) {
            throw new BusinessException(400, "你们已经是好友了");
        }
        createBidirectionalFriendship(user, inviter);
        invite.setStatus(FriendInvite.InviteStatus.USED);
        friendInvites.save(invite);
        return new ApiDtos.FriendInviteResponse(
                invite.getCode(), invite.getCreatedAt(),
                invite.getStatus().name(), invite.getExpiresAt());
    }

    // ─── 好友申请 ──────────────────────────────────────────────

    @Transactional
    public ApiDtos.FriendRequestResponse sendRequest(UserAccount fromUser, ApiDtos.FriendRequestSend req) {
        UserAccount toUser = users.findByPhone(req.phone())
                .orElseThrow(() -> new BusinessException(404, "该用户不存在"));
        if (toUser.getId().equals(fromUser.getId())) {
            throw new BusinessException(400, "不能给自己发送好友申请");
        }
        if (friendships.existsByUserAndFriendAndStatus(fromUser, toUser, Friendship.FriendshipStatus.ACTIVE)) {
            throw new BusinessException(400, "你们已经是好友了");
        }
        if (friendships.existsByUserAndFriendAndStatus(toUser, fromUser, Friendship.FriendshipStatus.BLOCKED)) {
            throw new BusinessException(400, "无法添加该用户为好友");
        }
        if (requests.existsByFromUserAndToUserAndStatus(fromUser, toUser, FriendRequest.RequestStatus.PENDING)) {
            throw new BusinessException(400, "已存在待处理的好友申请");
        }
        FriendRequest request = new FriendRequest();
        request.setFromUser(fromUser);
        request.setToUser(toUser);
        request.setMessage(req.message() != null ? req.message() : "");
        request.setStatus(FriendRequest.RequestStatus.PENDING);
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());
        requests.save(request);
        return toRequestResponse(request);
    }

    @Transactional
    public ApiDtos.FriendRequestResponse acceptRequest(UserAccount user, Long requestId) {
        FriendRequest request = requests.findById(requestId)
                .orElseThrow(() -> new BusinessException(404, "好友申请不存在"));
        if (!request.getToUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权处理该好友申请");
        }
        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new BusinessException(400, "该申请已被处理");
        }
        createBidirectionalFriendship(request.getFromUser(), request.getToUser());
        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        request.setUpdatedAt(Instant.now());
        requests.save(request);
        return toRequestResponse(request);
    }

    @Transactional
    public ApiDtos.FriendRequestResponse rejectRequest(UserAccount user, Long requestId) {
        FriendRequest request = requests.findById(requestId)
                .orElseThrow(() -> new BusinessException(404, "好友申请不存在"));
        if (!request.getToUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权处理该好友申请");
        }
        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new BusinessException(400, "该申请已被处理");
        }
        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        request.setUpdatedAt(Instant.now());
        requests.save(request);
        return toRequestResponse(request);
    }

    @Transactional
    public ApiDtos.FriendRequestResponse blockRequest(UserAccount user, Long requestId) {
        FriendRequest request = requests.findById(requestId)
                .orElseThrow(() -> new BusinessException(404, "好友申请不存在"));
        if (!request.getToUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权处理该好友申请");
        }
        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new BusinessException(400, "该申请已被处理");
        }
        request.setStatus(FriendRequest.RequestStatus.BLOCKED);
        request.setUpdatedAt(Instant.now());
        requests.save(request);

        // 创建拉黑关系记录
        createBlockRecord(user, request.getFromUser());
        return toRequestResponse(request);
    }

    @Transactional
    public void cancelRequest(UserAccount user, Long requestId) {
        FriendRequest request = requests.findById(requestId)
                .orElseThrow(() -> new BusinessException(404, "好友申请不存在"));
        if (!request.getFromUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "无权取消该好友申请");
        }
        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new BusinessException(400, "该申请已被处理，无法取消");
        }
        requests.delete(request);
    }

    @Transactional
    public void deleteFriend(UserAccount user, Long friendId) {
        UserAccount friend = users.findById(friendId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Friendship f1 = friendships.findByUserAndFriend(user, friend)
                .orElse(null);
        Friendship f2 = friendships.findByUserAndFriend(friend, user)
                .orElse(null);

        if (f1 != null) {
            f1.setStatus(Friendship.FriendshipStatus.DELETED);
            f1.setUpdatedAt(Instant.now());
            friendships.save(f1);
        }
        if (f2 != null) {
            f2.setStatus(Friendship.FriendshipStatus.DELETED);
            f2.setUpdatedAt(Instant.now());
            friendships.save(f2);
        }
    }

    @Transactional
    public void blockFriend(UserAccount user, Long friendId) {
        UserAccount friend = users.findById(friendId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        Friendship f = friendships.findByUserAndFriend(user, friend)
                .orElseThrow(() -> new BusinessException(404, "好友关系不存在"));
        f.setStatus(Friendship.FriendshipStatus.BLOCKED);
        f.setUpdatedAt(Instant.now());
        friendships.save(f);

        // 同时断开对方的记录
        friendships.findByUserAndFriend(friend, user).ifPresent(f2 -> {
            f2.setStatus(Friendship.FriendshipStatus.BLOCKED);
            f2.setUpdatedAt(Instant.now());
            friendships.save(f2);
        });
    }

    @Transactional
    public void unblockFriend(UserAccount user, Long friendId) {
        UserAccount friend = users.findById(friendId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        Friendship f = friendships.findByUserAndFriend(user, friend)
                .orElseThrow(() -> new BusinessException(404, "拉黑关系不存在"));
        if (f.getStatus() != Friendship.FriendshipStatus.BLOCKED) {
            throw new BusinessException(400, "该用户未被拉黑");
        }
        f.setStatus(Friendship.FriendshipStatus.DELETED);
        f.setUpdatedAt(Instant.now());
        friendships.save(f);

        friendships.findByUserAndFriend(friend, user).ifPresent(f2 -> {
            f2.setStatus(Friendship.FriendshipStatus.DELETED);
            f2.setUpdatedAt(Instant.now());
            friendships.save(f2);
        });
    }

    // ─── 查询 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApiDtos.UserProfileResponse> listFriends(UserAccount user) {
        return friendships.findByUserAndStatusOrderByUpdatedAtDesc(user, Friendship.FriendshipStatus.ACTIVE)
                .stream()
                .map(f -> DtoMapper.user(f.getFriend()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.FriendInviteResponse> listFriendInvites(UserAccount owner) {
        return friendInvites.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(i -> new ApiDtos.FriendInviteResponse(
                        i.getCode(), i.getCreatedAt(),
                        i.getStatus().name(), i.getExpiresAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.FriendRequestResponse> listRequests(UserAccount user) {
        List<FriendRequest> incoming = requests.findByToUserAndStatusOrderByCreatedAtDesc(
                user, FriendRequest.RequestStatus.PENDING);
        List<FriendRequest> outgoing = requests.findByFromUserAndStatusOrderByCreatedAtDesc(
                user, FriendRequest.RequestStatus.PENDING);
        List<ApiDtos.FriendRequestResponse> all = new java.util.ArrayList<>();
        all.addAll(incoming.stream().map(this::toRequestResponse).toList());
        all.addAll(outgoing.stream().map(this::toRequestResponse).toList());
        return all;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.UserProfileResponse> listBlocked(UserAccount user) {
        return friendships.findByUserAndStatusOrderByUpdatedAtDesc(user, Friendship.FriendshipStatus.BLOCKED)
                .stream()
                .map(f -> DtoMapper.user(f.getFriend()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.UserProfileResponse searchByPhone(UserAccount currentUser, String phone) {
        return users.findByPhone(phone)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .map(DtoMapper::user)
                .orElseThrow(() -> new BusinessException(404, "该用户不存在"));
    }

    // ─── 内部工具方法 ──────────────────────────────────────────

    private void createBidirectionalFriendship(UserAccount userA, UserAccount userB) {
        Instant now = Instant.now();
        upsertFriendship(userA, userB, Friendship.FriendshipStatus.ACTIVE, now);
        upsertFriendship(userB, userA, Friendship.FriendshipStatus.ACTIVE, now);
    }

    private void upsertFriendship(UserAccount user, UserAccount friend, Friendship.FriendshipStatus status, Instant now) {
        Friendship existing = friendships.findByUserAndFriend(user, friend).orElse(null);
        if (existing != null) {
            existing.setStatus(status);
            existing.setUpdatedAt(now);
            friendships.save(existing);
        } else {
            Friendship f = new Friendship();
            f.setUser(user);
            f.setFriend(friend);
            f.setStatus(status);
            f.setCreatedAt(now);
            f.setUpdatedAt(now);
            friendships.save(f);
        }
    }

    private void createBlockRecord(UserAccount blocker, UserAccount blocked) {
        Instant now = Instant.now();
        Friendship f = friendships.findByUserAndFriend(blocker, blocked).orElse(null);
        if (f == null) {
            f = new Friendship();
            f.setUser(blocker);
            f.setFriend(blocked);
            f.setCreatedAt(now);
        }
        f.setStatus(Friendship.FriendshipStatus.BLOCKED);
        f.setUpdatedAt(now);
        friendships.save(f);
    }

    private ApiDtos.FriendRequestResponse toRequestResponse(FriendRequest req) {
        return new ApiDtos.FriendRequestResponse(
                req.getId(),
                DtoMapper.user(req.getFromUser()),
                DtoMapper.user(req.getToUser()),
                req.getStatus().name(),
                req.getMessage(),
                req.getCreatedAt());
    }
}
