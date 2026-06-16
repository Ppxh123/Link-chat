package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.dto.request.CreateGroupRequest;
import com.linkchat.server.dto.request.InviteMemberRequest;
import com.linkchat.server.dto.request.UpdateGroupAnnouncementRequest;
import com.linkchat.server.dto.response.GroupInfoResponse;
import com.linkchat.server.dto.response.UserInfoResponse;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.FileService;
import com.linkchat.server.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final FileService fileService;
    private final SecurityUtils securityUtils;

    @PostMapping("/create")
    public Result<GroupInfoResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        return Result.success(groupService.createGroup(userId, request));
    }

    @GetMapping("/{groupId}")
    public Result<GroupInfoResponse> getGroupInfo(@PathVariable Long groupId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.checkMembership(userId, groupId);
        return Result.success(groupService.getGroupInfo(groupId));
    }

    @DeleteMapping("/{groupId}")
    public Result<Void> dismissGroup(@PathVariable Long groupId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.dismissGroup(userId, groupId);
        return Result.success();
    }

    @DeleteMapping("/{groupId}/leave")
    public Result<Void> leaveGroup(@PathVariable Long groupId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.leaveGroup(userId, groupId);
        return Result.success();
    }

    @PostMapping("/{groupId}/invite")
    public Result<Void> inviteMembers(@PathVariable Long groupId, @Valid @RequestBody InviteMemberRequest request) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.inviteMembers(userId, groupId, request.getUserIds());
        return Result.success();
    }

    @DeleteMapping("/{groupId}/member/{memberId}")
    public Result<Void> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.removeMember(userId, groupId, memberId);
        return Result.success();
    }

    @PutMapping("/{groupId}/admin/{memberId}")
    public Result<Void> setAdmin(@PathVariable Long groupId, @PathVariable Long memberId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.setAdmin(userId, groupId, memberId);
        return Result.success();
    }

    @DeleteMapping("/{groupId}/admin/{memberId}")
    public Result<Void> removeAdmin(@PathVariable Long groupId, @PathVariable Long memberId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.removeAdmin(userId, groupId, memberId);
        return Result.success();
    }

    @PutMapping("/{groupId}/transfer/{newOwnerId}")
    public Result<Void> transferOwnership(@PathVariable Long groupId, @PathVariable Long newOwnerId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.transferOwnership(userId, groupId, newOwnerId);
        return Result.success();
    }

    @PutMapping("/{groupId}/name")
    public Result<Void> updateGroupName(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.updateGroupName(userId, groupId, body.get("name"));
        return Result.success();
    }

    @PutMapping("/{groupId}/announcement")
    public Result<Void> updateAnnouncement(@PathVariable Long groupId,
                                           @Valid @RequestBody UpdateGroupAnnouncementRequest request) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.updateAnnouncement(userId, groupId, request.getAnnouncement());
        return Result.success();
    }

    @PostMapping("/{groupId}/avatar")
    public Result<Map<String, String>> uploadGroupAvatar(@PathVariable Long groupId,
                                                          @RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        Map<String, String> result = fileService.uploadAvatar(userId, file);
        groupService.updateGroupAvatar(userId, groupId, result.get("avatarUrl"));
        return Result.success(result);
    }

    @PutMapping("/{groupId}/mute-all")
    public Result<Void> muteAll(@PathVariable Long groupId, @RequestBody Map<String, Boolean> body) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.muteAll(userId, groupId, body.getOrDefault("muted", true));
        return Result.success();
    }

    @PutMapping("/{groupId}/mute-member/{memberId}")
    public Result<Void> muteMember(@PathVariable Long groupId, @PathVariable Long memberId,
                                    @RequestBody Map<String, Boolean> body) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.muteMember(userId, groupId, memberId, body.getOrDefault("muted", true));
        return Result.success();
    }

    @GetMapping("/{groupId}/members")
    public Result<List<UserInfoResponse>> getGroupMembers(@PathVariable Long groupId) {
        Long userId = securityUtils.getCurrentUserIdRequired();
        groupService.checkMembership(userId, groupId);
        return Result.success(groupService.getGroupMembers(groupId));
    }

    @GetMapping("/list")
    public Result<List<GroupInfoResponse>> getUserGroups() {
        Long userId = securityUtils.getCurrentUserIdRequired();
        return Result.success(groupService.getUserGroups(userId));
    }
}
