package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.dto.request.UpdateProfileRequest;
import com.linkchat.server.dto.response.UserInfoResponse;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.FileService;
import com.linkchat.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileService fileService;
    private final SecurityUtils securityUtils;

    @GetMapping("/profile")
    public Result<UserInfoResponse> getProfile() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(userService.getProfile(userId));
    }

    @GetMapping("/profile/{userId}")
    public Result<UserInfoResponse> getUserProfile(@PathVariable Long userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userService.updateProfile(userId, request);
        return Result.success();
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, String> result = fileService.uploadAvatar(userId, file);
        userService.updateAvatar(userId, result.get("avatarUrl"));
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<List<UserInfoResponse>> searchUsers(@RequestParam String keyword) {
        return Result.success(userService.searchUsers(keyword));
    }
}