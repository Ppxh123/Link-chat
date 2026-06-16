package com.linkchat.server.service;

import com.linkchat.server.dto.request.CreateGroupRequest;
import com.linkchat.server.dto.response.GroupInfoResponse;
import com.linkchat.server.dto.response.UserInfoResponse;

import java.util.List;

public interface GroupService {
    void checkMembership(Long userId, Long groupId);
    GroupInfoResponse createGroup(Long ownerId, CreateGroupRequest request);
    GroupInfoResponse getGroupInfo(Long groupId);
    void dismissGroup(Long userId, Long groupId);
    void leaveGroup(Long userId, Long groupId);
    void inviteMembers(Long operatorId, Long groupId, List<Long> memberIds);
    void removeMember(Long operatorId, Long groupId, Long memberId);
    void setAdmin(Long ownerId, Long groupId, Long memberId);
    void removeAdmin(Long ownerId, Long groupId, Long memberId);
    void transferOwnership(Long ownerId, Long groupId, Long newOwnerId);
    void updateGroupName(Long userId, Long groupId, String name);
    void updateAnnouncement(Long userId, Long groupId, String announcement);
    void updateGroupAvatar(Long userId, Long groupId, String avatarUrl);
    void muteAll(Long ownerId, Long groupId, boolean muted);
    void muteMember(Long operatorId, Long groupId, Long memberId, boolean muted);
    List<UserInfoResponse> getGroupMembers(Long groupId);
    List<GroupInfoResponse> getUserGroups(Long userId);
}
