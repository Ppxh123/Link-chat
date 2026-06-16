package com.linkchat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.Constants;
import com.linkchat.server.common.ResultCode;
import com.linkchat.server.dto.response.FriendListResponse;
import com.linkchat.server.entity.Friend;
import com.linkchat.server.entity.User;
import com.linkchat.server.repository.FriendRepository;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    @Override
    @Transactional
    public void addFriend(Long userId, String keyword) {
        // 防御性检查：确保 userId 不为空
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 查找目标用户
        User target = userRepository.selectOne(
                new LambdaQueryWrapper<User>()
                        .and(w -> w.eq(User::getEmail, keyword)
                                .or().eq(User::getUserCode, keyword)));

        if (target == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (target.getId().equals(userId)) {
            throw new BusinessException(ResultCode.FRIEND_SELF);
        }

        // 检查是否已经是好友
        boolean exists = friendRepository.exists(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, target.getId())
                        .eq(Friend::getStatus, Constants.FRIEND_ACCEPTED));

        if (exists) {
            throw new BusinessException(ResultCode.FRIEND_ALREADY_EXISTS);
        }

        // 检查是否已有待处理申请
        boolean pending = friendRepository.exists(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, target.getId())
                        .eq(Friend::getStatus, Constants.FRIEND_PENDING));

        if (pending) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_EXISTS);
        }

        // 检查是否有已删除/已拒绝的旧记录，如果有则更新而非插入（避免唯一键冲突）
        Friend oldRecord = friendRepository.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, target.getId())
                        .in(Friend::getStatus, Constants.FRIEND_REJECTED, Constants.FRIEND_DELETED));

        if (oldRecord != null) {
            oldRecord.setStatus(Constants.FRIEND_PENDING);
            friendRepository.updateById(oldRecord);
        } else {
            // 发送好友申请
            Friend friend = new Friend();
            friend.setUserId(userId);
            friend.setFriendId(target.getId());
            friend.setStatus(Constants.FRIEND_PENDING);
            friendRepository.insert(friend);
        }

        // 通过 WebSocket 通知目标用户有新好友申请
        notifyFriendRequest(target.getId());

        log.info("好友申请已发送: userId={}, targetId={}", userId, target.getId());
    }

    /**
     * 通过 WebSocket 推送好友申请通知给目标用户
     */
    private void notifyFriendRequest(Long targetUserId) {
        try {
            var session = com.linkchat.server.websocket.WebSocketServer.getSession(targetUserId);
            if (session != null && session.isOpen()) {
                var notifyMsg = new cn.hutool.json.JSONObject();
                notifyMsg.set("type", "friend_request");
                var payload = new cn.hutool.json.JSONObject();
                payload.set("message", "您有新的好友申请");
                notifyMsg.set("payload", payload);
                com.linkchat.server.websocket.WebSocketServer.sendMessage(session, notifyMsg.toString());
            }
        } catch (Exception e) {
            log.warn("好友申请WebSocket通知失败: targetUserId={}", targetUserId, e);
        }
    }

    @Override
    public List<FriendListResponse> getPendingRequests(Long userId) {
        // 查询所有发给我的待处理申请（friendId = userId 且 status = PENDING）
        List<Friend> pendingRequests = friendRepository.selectList(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getFriendId, userId)
                        .eq(Friend::getStatus, Constants.FRIEND_PENDING));

        if (pendingRequests.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取申请发起者的用户信息
        List<Long> requesterIds = pendingRequests.stream()
                .map(Friend::getUserId)
                .collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.selectBatchIds(requesterIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return pendingRequests.stream().map(f -> {
            User u = userMap.get(f.getUserId());
            if (u == null) {
                return null;
            }
            return FriendListResponse.builder()
                    .friendId(u.getId())
                    .nickname(u.getNickname())
                    .userCode(u.getUserCode())
                    .avatarUrl(u.getAvatarUrl())
                    .signature(u.getSignature())
                    .remark(f.getRemark())
                    .status(u.getStatus())
                    .lastOnline(u.getLastOnline())
                    .build();
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void acceptFriend(Long userId, Long friendId) {
        // 查找待处理的申请（对方发给我的）
        Friend friend = friendRepository.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, friendId)
                        .eq(Friend::getFriendId, userId)
                        .eq(Friend::getStatus, Constants.FRIEND_PENDING));

        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_NOT_FOUND);
        }

        friend.setStatus(Constants.FRIEND_ACCEPTED);
        friendRepository.updateById(friend);

        // 建立双向好友关系 — 需检查反向记录是否已存在（例如双方互相申请的场景），
        // 避免 uk_user_friend 唯一键冲突
        Friend existingReverse = friendRepository.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, friendId));

        if (existingReverse != null) {
            // 反向记录已存在（可能是对方也发来的 PENDING，或旧 REJECTED/DELETED），
            // 直接更新为 ACCEPTED 而非插入
            existingReverse.setStatus(Constants.FRIEND_ACCEPTED);
            friendRepository.updateById(existingReverse);
        } else {
            Friend reverse = new Friend();
            reverse.setUserId(userId);
            reverse.setFriendId(friendId);
            reverse.setStatus(Constants.FRIEND_ACCEPTED);
            friendRepository.insert(reverse);
        }

        log.info("好友申请已同意: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public void rejectFriend(Long userId, Long friendId) {
        Friend friend = friendRepository.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, friendId)
                        .eq(Friend::getFriendId, userId)
                        .eq(Friend::getStatus, Constants.FRIEND_PENDING));

        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_NOT_FOUND);
        }

        friend.setStatus(Constants.FRIEND_REJECTED);
        friendRepository.updateById(friend);

        log.info("好友申请已拒绝: userId={}, friendId={}", userId, friendId);
    }

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Friend friend = friendRepository.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, friendId)
                        .eq(Friend::getStatus, Constants.FRIEND_ACCEPTED));

        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_NOT_FOUND);
        }

        friend.setStatus(Constants.FRIEND_DELETED);
        friendRepository.updateById(friend);

        // 删除反向关系
        Friend reverse = friendRepository.selectOne(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, friendId)
                        .eq(Friend::getFriendId, userId)
                        .eq(Friend::getStatus, Constants.FRIEND_ACCEPTED));

        if (reverse != null) {
            reverse.setStatus(Constants.FRIEND_DELETED);
            friendRepository.updateById(reverse);
        }

        log.info("好友已删除: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public List<FriendListResponse> getFriendList(Long userId) {
        List<Friend> friends = friendRepository.selectList(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getStatus, Constants.FRIEND_ACCEPTED));

        if (friends.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> friendIds = friends.stream().map(Friend::getFriendId).collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.selectBatchIds(friendIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return friends.stream().map(f -> {
            User u = userMap.get(f.getFriendId());
            if (u == null) {
                return null;
            }
            return FriendListResponse.builder()
                    .friendId(u.getId())
                    .nickname(u.getNickname())
                    .userCode(u.getUserCode())
                    .avatarUrl(u.getAvatarUrl())
                    .signature(u.getSignature())
                    .remark(f.getRemark())
                    .status(u.getStatus())
                    .lastOnline(u.getLastOnline())
                    .build();
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<FriendListResponse> searchFriends(Long userId, String keyword) {
        List<FriendListResponse> allFriends = getFriendList(userId);
        return allFriends.stream()
                .filter(f -> f.getNickname().contains(keyword)
                        || f.getUserCode().contains(keyword))
                .collect(Collectors.toList());
    }
}