package com.linkchat.server.service.impl;

import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.ResultCode;
import com.linkchat.server.dto.request.UpdateProfileRequest;
import com.linkchat.server.dto.response.UserInfoResponse;
import com.linkchat.server.entity.User;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserInfoResponse getProfile(Long userId) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toUserInfo(user);
    }

    @Override
    public List<UserInfoResponse> searchUsers(String keyword) {
        // 转义 LIKE 通配符，防止搜索 % 或 _ 时匹配所有记录
        String escapedKeyword = escapeLikeWildcards(keyword);
        List<User> users = userRepository.searchByKeyword(escapedKeyword);
        return users.stream().map(this::toUserInfo).collect(Collectors.toList());
    }

    /**
     * 转义 SQL LIKE 通配符
     */
    private String escapeLikeWildcards(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        userRepository.updateById(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setAvatarUrl(avatarUrl);
        userRepository.updateById(user);
    }

    private UserInfoResponse toUserInfo(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .userCode(user.getUserCode())
                .avatarUrl(user.getAvatarUrl())
                .signature(user.getSignature())
                .status(user.getStatus())
                .lastOnline(user.getLastOnline())
                .build();
    }
}