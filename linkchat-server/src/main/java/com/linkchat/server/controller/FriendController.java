package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.dto.request.AddFriendRequest;
import com.linkchat.server.dto.response.FriendListResponse;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final SecurityUtils securityUtils;

    @PostMapping("/add")
    public Result<Void> addFriend(@Valid @RequestBody AddFriendRequest request) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        friendService.addFriend(userId, request.getKeyword());
        return Result.success();
    }

    @GetMapping("/requests")
    public Result<List<FriendListResponse>> getPendingRequests() {
        Long userId = securityUtils.getCurrentUserIdRequired();
        return Result.success(friendService.getPendingRequests(userId));
    }

    @PutMapping("/accept/{friendId}")
    public Result<Void> acceptFriend(@PathVariable Long friendId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        friendService.acceptFriend(userId, friendId);
        return Result.success();
    }

    @PutMapping("/reject/{friendId}")
    public Result<Void> rejectFriend(@PathVariable Long friendId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        friendService.rejectFriend(userId, friendId);
        return Result.success();
    }

    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@PathVariable Long friendId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        friendService.deleteFriend(userId, friendId);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<FriendListResponse>> getFriendList() {
        Long userId = securityUtils.getCurrentUserIdRequired();
        return Result.success(friendService.getFriendList(userId));
    }

    @GetMapping("/search")
    public Result<List<FriendListResponse>> searchFriends(@RequestParam String keyword) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        return Result.success(friendService.searchFriends(userId, keyword));
    }
}