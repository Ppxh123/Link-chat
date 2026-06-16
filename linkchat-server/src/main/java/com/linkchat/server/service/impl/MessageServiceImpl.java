package com.linkchat.server.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.Constants;
import com.linkchat.server.common.ResultCode;
import com.linkchat.server.dto.request.SendMessageRequest;
import com.linkchat.server.dto.response.MessageResponse;
import com.linkchat.server.entity.Message;
import com.linkchat.server.entity.OfflineMessage;
import com.linkchat.server.entity.User;
import com.linkchat.server.entity.GroupChat;
import com.linkchat.server.entity.GroupMember;
import com.linkchat.server.repository.GroupChatRepository;
import com.linkchat.server.repository.GroupMemberRepository;
import com.linkchat.server.repository.MessageRepository;
import com.linkchat.server.repository.OfflineMessageRepository;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.service.MessageService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.linkchat.server.util.RedisUtils;
import com.linkchat.server.util.SnowflakeIdWorker;
import com.linkchat.server.websocket.WebSocketServer;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OfflineMessageRepository offlineMessageRepository;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final GroupChatRepository groupChatRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        long dedupSeed = senderId ^ (request.getReceiverId() != null ? request.getReceiverId() : 0L)
                ^ (request.getGroupId() != null ? request.getGroupId() : 0L);
        String contentFingerprint = request.getContent() != null ? request.getContent() : "";
        String dedupKey = Constants.REDIS_MSG_DEDUP + dedupSeed + "_" + contentFingerprint.hashCode();
        if (!redisUtils.setIfAbsent(dedupKey, "1", 5, TimeUnit.SECONDS)) {
            log.warn("重复消息: senderId={}", senderId);
            throw new BusinessException(ResultCode.MESSAGE_SEND_FAILED);
        }

        // 群聊禁言检查
        if (request.getGroupId() != null) {
            GroupChat group = groupChatRepository.selectById(request.getGroupId());
            if (group != null && group.getIsMuted() != null && group.getIsMuted() == 1) {
                GroupMember member = groupMemberRepository.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GroupMember>()
                                .eq(GroupMember::getGroupId, request.getGroupId())
                                .eq(GroupMember::getUserId, senderId));
                if (member == null || member.getRole() == Constants.ROLE_MEMBER)
                    throw new BusinessException(ResultCode.GROUP_MUTED);
            }
        }

        // 生成消息ID（雪花算法）
        long messageId = snowflakeIdWorker.nextId();

        Message message = new Message();
        message.setId(messageId);
        message.setSenderId(senderId);
        message.setReceiverId(request.getGroupId() != null ? null : request.getReceiverId());
        message.setGroupId(request.getGroupId());
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setFileUrl(request.getFileUrl());
        message.setFileName(request.getFileName());
        message.setFileSize(request.getFileSize());
        message.setFileMime(request.getFileMime());
        message.setQuotedMsgId(request.getQuotedMsgId());
        message.setAckStatus(Constants.ACK_SENT);
        messageRepository.insert(message);

        // 优先通过 WebSocket 实时推送；Session 不存在或已关闭时存为离线消息
        if (request.getReceiverId() != null) {
            Session receiverSession = WebSocketServer.getSession(request.getReceiverId());
            if (receiverSession != null && receiverSession.isOpen()) {
                // WebSocket 在线 → 实时推送
                MessageResponse response = toMessageResponse(message);
                WebSocketServer.sendMessage(receiverSession, buildWsMessage("message", response));
                log.info("消息已实时推送: messageId={}, receiverId={}", messageId, request.getReceiverId());
            } else {
                // WebSocket 不在线 → 存离线消息
                OfflineMessage offline = new OfflineMessage();
                offline.setMessageId(messageId);
                offline.setReceiverId(request.getReceiverId());
                offline.setMessageType(request.getMessageType());
                offline.setContent(request.getContent());
                offline.setFileUrl(request.getFileUrl());
                offline.setIsPushed(0);
                offlineMessageRepository.insert(offline);

                String offlineKey = Constants.REDIS_OFFLINE_MSG + request.getReceiverId();
                redisUtils.leftPush(offlineKey, String.valueOf(messageId));
                redisUtils.expire(offlineKey, 7, TimeUnit.DAYS);
                log.info("接收者离线，消息已暂存: messageId={}, receiverId={}", messageId, request.getReceiverId());
            }
        }

        // 群聊消息：通过 WebSocket 实时推送给群内所有在线成员
        if (request.getGroupId() != null) {
            MessageResponse response = toMessageResponse(message);
            WebSocketServer.broadcastToGroup(request.getGroupId(), buildWsMessage("message", response), senderId);
            log.info("群聊消息已实时广播: messageId={}, groupId={}", messageId, request.getGroupId());
        }

        log.info("消息已发送: messageId={}, senderId={}, receiverId={}", messageId, senderId, request.getReceiverId());
        return toMessageResponse(message);
    }

    /**
     * 使用 Jackson 构建 WebSocket 消息 JSON，确保 Long 字段序列化为字符串（避免 JS 精度丢失）
     */
    private String buildWsMessage(String type, Object payload) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", type);
            msg.put("payload", payload);
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            log.error("构建WebSocket消息失败: type={}", type, e);
            return "{}";
        }
    }

    @Override
    public List<MessageResponse> getChatHistory(Long userId, Long peerId, Long groupId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null) {
            wrapper.eq(Message::getGroupId, groupId);
        } else {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(Message::getSenderId, userId).eq(Message::getReceiverId, peerId))
                    .or(w2 -> w2.eq(Message::getSenderId, peerId).eq(Message::getReceiverId, userId)));
        }

        wrapper.eq(Message::getIsDeleted, 0)
                .orderByDesc(Message::getCreatedAt);

        Page<Message> messagePage = new Page<>(page, size);
        Page<Message> result = messageRepository.selectPage(messagePage, wrapper);

        return result.getRecords().stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        Message message = messageRepository.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }

        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_SENDER);
        }

        // 2分钟内可撤回
        long elapsed = System.currentTimeMillis() -
                message.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (elapsed > Constants.RECALL_TIMEOUT_MS) {
            throw new BusinessException(ResultCode.MESSAGE_RECALL_TIMEOUT);
        }

        message.setIsRecalled(1);
        messageRepository.updateById(message);
        log.info("消息已撤回: messageId={}, userId={}", messageId, userId);

        // 通过 WebSocket 广播撤回事件给所有相关用户
        Map<String, Object> recallPayload = new HashMap<>();
        recallPayload.put("messageId", String.valueOf(messageId));
        recallPayload.put("senderId", String.valueOf(userId));
        String recallMsg = buildWsMessage("recall", recallPayload);

        if (message.getGroupId() != null) {
            // 群聊：广播给群内所有在线成员（包含发送者本人）
            log.info("撤回广播(群聊): groupId={}, messageId={}", message.getGroupId(), messageId);
            WebSocketServer.broadcastToGroup(message.getGroupId(), recallMsg.toString(), null);
        } else {
            // 单聊：发给发送者和接收者
            log.info("撤回广播(单聊): senderId={}, receiverId={}, messageId={}", userId, message.getReceiverId(), messageId);
            jakarta.websocket.Session senderSession = WebSocketServer.getSession(userId);
            if (senderSession != null) {
                WebSocketServer.sendMessage(senderSession, recallMsg);
                log.info("撤回广播已发送给发送者: userId={}", userId);
            }
            if (message.getReceiverId() != null) {
                jakarta.websocket.Session receiverSession = WebSocketServer.getSession(message.getReceiverId());
                if (receiverSession != null) {
                    WebSocketServer.sendMessage(receiverSession, recallMsg);
                    log.info("撤回广播已发送给接收者: userId={}", message.getReceiverId());
                }
            }
        }
    }

    @Override
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageRepository.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }

        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_SENDER);
        }

        message.setIsDeleted(1);
        messageRepository.updateById(message);
        log.info("消息已删除: messageId={}, userId={}", messageId, userId);
    }

    @Override
    public MessageResponse forwardMessage(Long userId, Long messageId, Long targetId) {
        Message original = messageRepository.selectById(messageId);
        if (original == null) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }

        // 权限校验：只有原始消息的参与者才能转发
        boolean permitted = original.getSenderId().equals(userId)
                || (original.getReceiverId() != null && original.getReceiverId().equals(userId));
        if (!permitted && original.getGroupId() != null) {
            permitted = groupMemberRepository.selectCount(
                    new LambdaQueryWrapper<GroupMember>()
                            .eq(GroupMember::getGroupId, original.getGroupId())
                            .eq(GroupMember::getUserId, userId)) > 0;
        }
        if (!permitted) {
            throw new BusinessException(ResultCode.MESSAGE_FORWARD_DENIED);
        }

        long newMessageId = snowflakeIdWorker.nextId();

        Message forwarded = new Message();
        forwarded.setId(newMessageId);
        forwarded.setSenderId(userId);
        forwarded.setReceiverId(targetId);
        forwarded.setMessageType(original.getMessageType());
        forwarded.setContent(original.getContent());
        forwarded.setFileUrl(original.getFileUrl());
        forwarded.setFileName(original.getFileName());
        forwarded.setFileSize(original.getFileSize());
        forwarded.setFileMime(original.getFileMime());
        forwarded.setAckStatus(Constants.ACK_SENT);
        messageRepository.insert(forwarded);

        log.info("消息已转发: originalId={}, newId={}, targetId={}", messageId, newMessageId, targetId);
        return toMessageResponse(forwarded);
    }

    @Override
    public List<MessageResponse> searchMessages(Long userId, Long peerId, String keyword) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Message::getSenderId, userId).eq(Message::getReceiverId, peerId))
                .or(w2 -> w2.eq(Message::getSenderId, peerId).eq(Message::getReceiverId, userId)))
                .like(Message::getContent, keyword)
                .eq(Message::getIsDeleted, 0)
                .orderByDesc(Message::getCreatedAt);

        return messageRepository.selectList(wrapper).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long userId, Long senderId) {
        // 批量将 sender 发给 userId 的所有未读消息标记为已读
        Message update = new Message();
        update.setAckStatus(Constants.ACK_READ);
        messageRepository.update(update,
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSenderId, senderId)
                        .eq(Message::getReceiverId, userId)
                        .ne(Message::getAckStatus, Constants.ACK_READ));
    }

    @Override
    public MessageResponse toMessageResponse(Message message) {
        User sender = userRepository.selectById(message.getSenderId());
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getNickname() : "未知用户")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                .receiverId(message.getReceiverId())
                .groupId(message.getGroupId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .fileMime(message.getFileMime())
                .isRecalled(message.getIsRecalled())
                .quotedMsgId(message.getQuotedMsgId())
                .ackStatus(message.getAckStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }
}