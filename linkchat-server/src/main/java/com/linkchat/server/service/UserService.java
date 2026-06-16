package com.linkchat.server.service;

import com.linkchat.server.dto.request.UpdateProfileRequest;
import com.linkchat.server.dto.response.UserInfoResponse;

import java.util.List;

public interface UserService {
    UserInfoResponse getProfile(Long userId);
    List<UserInfoResponse> searchUsers(String keyword);
    void updateProfile(Long userId, UpdateProfileRequest request);
    void updateAvatar(Long userId, String avatarUrl);
}