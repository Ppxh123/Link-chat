package com.linkchat.server.service;

import com.linkchat.server.dto.response.FriendListResponse;

import java.util.List;

public interface FriendService {
    void addFriend(Long userId, String keyword);
    void acceptFriend(Long userId, Long friendId);
    void rejectFriend(Long userId, Long friendId);
    void deleteFriend(Long userId, Long friendId);
    List<FriendListResponse> getFriendList(Long userId);
    List<FriendListResponse> getPendingRequests(Long userId);
    List<FriendListResponse> searchFriends(Long userId, String keyword);
}