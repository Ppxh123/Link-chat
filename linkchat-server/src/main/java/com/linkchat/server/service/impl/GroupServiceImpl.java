package com.linkchat.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.Constants;
import com.linkchat.server.common.ResultCode;
import com.linkchat.server.dto.request.CreateGroupRequest;
import com.linkchat.server.dto.response.GroupInfoResponse;
import com.linkchat.server.dto.response.UserInfoResponse;
import com.linkchat.server.entity.GroupChat;
import com.linkchat.server.entity.GroupMember;
import com.linkchat.server.entity.User;
import com.linkchat.server.repository.GroupChatRepository;
import com.linkchat.server.repository.GroupMemberRepository;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupChatRepository groupChatRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public GroupInfoResponse createGroup(Long ownerId, CreateGroupRequest request) {
        GroupChat group = new GroupChat();
        group.setName(request.getName());
        group.setOwnerId(ownerId);
        group.setMemberCount(request.getMemberIds().size() + 1); // +1 for owner
        groupChatRepository.insert(group);

        // 群主加入
        GroupMember owner = new GroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(ownerId);
        owner.setRole(Constants.ROLE_OWNER);
        owner.setJoinedAt(LocalDateTime.now());
        groupMemberRepository.insert(owner);

        // 邀请成员
        for (Long memberId : request.getMemberIds()) {
            GroupMember gm = new GroupMember();
            gm.setGroupId(group.getId());
            gm.setUserId(memberId);
            gm.setRole(Constants.ROLE_MEMBER);
            gm.setJoinedAt(LocalDateTime.now());
            groupMemberRepository.insert(gm);
        }

        log.info("群聊已创建: groupId={}, ownerId={}, members={}", group.getId(), ownerId, request.getMemberIds());
        return toGroupInfo(group, Constants.ROLE_OWNER);
    }

    @Override
    public GroupInfoResponse getGroupInfo(Long groupId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        Long currentUserId = SecurityUtils.getCurrentUserIdSafely();
        int role = -1;
        if (currentUserId != null) {
            GroupMember member = groupMemberRepository.selectOne(
                    new LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, groupId)
                            .eq(GroupMember::getUserId, currentUserId));
            role = member != null ? member.getRole() : -1;
        }

        return toGroupInfo(group, role);
    }

    @Override
    @Transactional
    public void dismissGroup(Long userId, Long groupId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        group.setIsDeleted(1);
        groupChatRepository.updateById(group);
        log.info("群聊已解散: groupId={}, userId={}", groupId, userId);
    }

    @Override
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember member = checkMember(groupId, userId);
        if (member.getRole() == Constants.ROLE_OWNER) {
            throw new BusinessException(ResultCode.GROUP_OWNER_CANNOT_LEAVE);
        }

        groupMemberRepository.deleteById(member.getId());
        group.setMemberCount(group.getMemberCount() - 1);
        groupChatRepository.updateById(group);

        log.info("已退出群聊: groupId={}, userId={}", groupId, userId);
    }

    @Override
    @Transactional
    public void inviteMembers(Long operatorId, Long groupId, List<Long> memberIds) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember operator = checkMember(groupId, operatorId);
        // 群主和管理员可以邀请，普通成员不可以
        if (operator.getRole() == Constants.ROLE_MEMBER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        int addedCount = 0;
        for (Long memberId : memberIds) {
            boolean exists = groupMemberRepository.exists(
                    new LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, groupId)
                            .eq(GroupMember::getUserId, memberId));
            if (exists) continue;

            GroupMember gm = new GroupMember();
            gm.setGroupId(groupId);
            gm.setUserId(memberId);
            gm.setRole(Constants.ROLE_MEMBER);
            gm.setJoinedAt(LocalDateTime.now());
            groupMemberRepository.insert(gm);
            addedCount++;
        }

        if (addedCount > 0) {
            group.setMemberCount(group.getMemberCount() + addedCount);
            groupChatRepository.updateById(group);
        }

        log.info("已邀请成员: groupId={}, memberIds={}, addedCount={}", groupId, memberIds, addedCount);
    }

    @Override
    @Transactional
    public void removeMember(Long operatorId, Long groupId, Long memberId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember operator = checkMember(groupId, operatorId);
        GroupMember target = checkMember(groupId, memberId);

        // 不能踢群主
        if (target.getRole() == Constants.ROLE_OWNER) {
            throw new BusinessException(ResultCode.GROUP_OWNER_CANNOT_BE_KICKED);
        }

        // 管理员不能踢其他管理员
        if (operator.getRole() == Constants.ROLE_ADMIN && target.getRole() == Constants.ROLE_ADMIN) {
            throw new BusinessException(ResultCode.GROUP_ADMIN_CANNOT_BE_KICKED);
        }

        // 普通成员不能踢人（只能自己退群）
        if (operator.getRole() == Constants.ROLE_MEMBER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        groupMemberRepository.deleteById(target.getId());
        group.setMemberCount(group.getMemberCount() - 1);
        groupChatRepository.updateById(group);

        log.info("已移除成员: groupId={}, memberId={}, operatorId={}", groupId, memberId, operatorId);
    }

    @Override
    @Transactional
    public void setAdmin(Long ownerId, Long groupId, Long memberId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        GroupMember member = checkMember(groupId, memberId);
        if (member.getRole() == Constants.ROLE_OWNER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION, "群主无需设置管理员");
        }
        member.setRole(Constants.ROLE_ADMIN);
        groupMemberRepository.updateById(member);

        log.info("已设置管理员: groupId={}, memberId={}", groupId, memberId);
    }

    @Override
    @Transactional
    public void removeAdmin(Long ownerId, Long groupId, Long memberId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        GroupMember member = checkMember(groupId, memberId);
        if (member.getRole() != Constants.ROLE_ADMIN) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION, "该成员不是管理员");
        }
        member.setRole(Constants.ROLE_MEMBER);
        groupMemberRepository.updateById(member);

        log.info("已撤销管理员: groupId={}, memberId={}", groupId, memberId);
    }

    @Override
    @Transactional
    public void transferOwnership(Long ownerId, Long groupId, Long newOwnerId) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        GroupMember newOwner = checkMember(groupId, newOwnerId);

        // 旧群主降为成员
        GroupMember oldOwner = checkMember(groupId, ownerId);
        oldOwner.setRole(Constants.ROLE_MEMBER);
        groupMemberRepository.updateById(oldOwner);

        // 新群主升为OWNER
        newOwner.setRole(Constants.ROLE_OWNER);
        groupMemberRepository.updateById(newOwner);

        // 更新群主ID
        group.setOwnerId(newOwnerId);
        groupChatRepository.updateById(group);

        log.info("群主已转让: groupId={}, oldOwner={}, newOwner={}", groupId, ownerId, newOwnerId);
    }

    @Override
    @Transactional
    public void updateGroupName(Long userId, Long groupId, String name) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember member = checkMember(groupId, userId);
        if (member.getRole() == Constants.ROLE_MEMBER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        group.setName(name);
        groupChatRepository.updateById(group);
        log.info("群名称已修改: groupId={}, name={}", groupId, name);
    }

    @Override
    @Transactional
    public void updateAnnouncement(Long userId, Long groupId, String announcement) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember member = checkMember(groupId, userId);
        if (member.getRole() == Constants.ROLE_MEMBER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        group.setAnnouncement(announcement);
        groupChatRepository.updateById(group);
        log.info("群公告已更新: groupId={}", groupId);
    }

    @Override
    @Transactional
    public void updateGroupAvatar(Long userId, Long groupId, String avatarUrl) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember member = checkMember(groupId, userId);
        if (member.getRole() == Constants.ROLE_MEMBER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        group.setAvatarUrl(avatarUrl);
        groupChatRepository.updateById(group);
    }

    @Override
    @Transactional
    public void muteAll(Long ownerId, Long groupId, boolean muted) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }
        if (!group.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        group.setIsMuted(muted ? 1 : 0);
        groupChatRepository.updateById(group);
        log.info("全员禁言: groupId={}, muted={}", groupId, muted);
    }

    @Override
    @Transactional
    public void muteMember(Long operatorId, Long groupId, Long memberId, boolean muted) {
        GroupChat group = groupChatRepository.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        GroupMember operator = checkMember(groupId, operatorId);

        // 只有群主和管理员可以禁言
        if (operator.getRole() == Constants.ROLE_MEMBER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION);
        }

        GroupMember target = checkMember(groupId, memberId);

        // 不能禁言群主
        if (target.getRole() == Constants.ROLE_OWNER) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION, "不能禁言群主");
        }

        // 管理员不能禁言其他管理员
        if (operator.getRole() == Constants.ROLE_ADMIN && target.getRole() == Constants.ROLE_ADMIN) {
            throw new BusinessException(ResultCode.GROUP_NO_PERMISSION, "管理员不能禁言其他管理员");
        }

        target.setIsMuted(muted ? 1 : 0);
        groupMemberRepository.updateById(target);
        log.info("成员禁言: groupId={}, memberId={}, muted={}", groupId, memberId, muted);
    }

    @Override
    public List<UserInfoResponse> getGroupMembers(Long groupId) {
        if (!groupChatRepository.exists(new LambdaQueryWrapper<GroupChat>().eq(GroupChat::getId, groupId))) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND);
        }

        List<GroupMember> members = groupMemberRepository.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));

        List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) return new ArrayList<>();

        return userRepository.selectBatchIds(userIds).stream().map(u -> {
            GroupMember gm = members.stream().filter(m -> m.getUserId().equals(u.getId())).findFirst().orElse(null);
            return UserInfoResponse.builder()
                    .id(u.getId())
                    .email(u.getEmail())
                    .nickname(u.getNickname())
                    .userCode(u.getUserCode())
                    .avatarUrl(u.getAvatarUrl())
                    .signature(u.getSignature())
                    .status(u.getStatus())
                    .lastOnline(u.getLastOnline())
                    .role(gm != null ? gm.getRole() : -1)
                    .isMuted(gm != null ? gm.getIsMuted() : 0)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<GroupInfoResponse> getUserGroups(Long userId) {
        List<GroupMember> members = groupMemberRepository.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getUserId, userId));

        if (members.isEmpty()) return new ArrayList<>();

        List<Long> groupIds = members.stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        List<GroupChat> groups = groupChatRepository.selectBatchIds(groupIds);

        return groups.stream()
                .filter(g -> g.getIsDeleted() == 0)
                .map(g -> {
                    GroupMember member = members.stream()
                            .filter(m -> m.getGroupId().equals(g.getId()))
                            .findFirst().orElse(null);
                    int role = member != null ? member.getRole() : -1;
                    return toGroupInfo(g, role);
                }).collect(Collectors.toList());
    }

    @Override
    public void checkMembership(Long userId, Long groupId) {
        checkMember(groupId, userId);
    }

    private GroupMember checkMember(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.selectOne(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, groupId)
                        .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ResultCode.GROUP_NOT_MEMBER);
        }
        return member;
    }

    private GroupInfoResponse toGroupInfo(GroupChat group, int myRole) {
        return GroupInfoResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .avatarUrl(group.getAvatarUrl())
                .ownerId(group.getOwnerId())
                .announcement(group.getAnnouncement())
                .memberCount(group.getMemberCount())
                .isMuted(group.getIsMuted())
                .myRole(myRole)
                .createdAt(group.getCreatedAt())
                .build();
    }
}
