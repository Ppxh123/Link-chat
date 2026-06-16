package com.linkchat.server.service.impl;

import com.linkchat.server.common.Constants;
import com.linkchat.server.dto.response.MessageResponse;
import com.linkchat.server.entity.Message;
import com.linkchat.server.entity.OfflineMessage;
import com.linkchat.server.entity.User;
import com.linkchat.server.repository.MessageRepository;
import com.linkchat.server.repository.OfflineMessageRepository;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.service.OfflineMessageService;
import com.linkchat.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineMessageServiceImpl implements OfflineMessageService {

    private final OfflineMessageRepository offlineMessageRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RedisUtils redisUtils;

    @Override
    @Transactional
    public void saveOfflineMessage(Long messageId, Long receiverId, String messageType, String content, String fileUrl) {
        // 保存到MySQL
        OfflineMessage offline = new OfflineMessage();
        offline.setMessageId(messageId);
        offline.setReceiverId(receiverId);
        offline.setMessageType(messageType);
        offline.setContent(content);
        offline.setFileUrl(fileUrl);
        offline.setIsPushed(0);
        offlineMessageRepository.insert(offline);

        // 保存到Redis离线消息队列
        String offlineKey = Constants.REDIS_OFFLINE_MSG + receiverId;
        redisUtils.leftPush(offlineKey, messageId.toString());
        redisUtils.expire(offlineKey, 7, TimeUnit.DAYS);

        log.info("离线消息已保存: messageId={}, receiverId={}", messageId, receiverId);
    }

    @Override
    @Transactional
    public List<MessageResponse> getAndClearOfflineMessages(Long userId) {
        // 从Redis获取离线消息ID列表
        String offlineKey = Constants.REDIS_OFFLINE_MSG + userId;
        List<Object> msgIds = redisUtils.range(offlineKey, 0, -1);

        List<MessageResponse> responses = new ArrayList<>();
        if (msgIds == null || msgIds.isEmpty()) {
            return responses;
        }

        for (Object msgId : msgIds) {
            try {
                long messageId = Long.parseLong(msgId.toString());
                Message message = messageRepository.selectById(messageId);
                if (message != null) {
                    User sender = userRepository.selectById(message.getSenderId());
                    responses.add(MessageResponse.builder()
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
                            .isRecalled(message.getIsRecalled())
                            .quotedMsgId(message.getQuotedMsgId())
                            .ackStatus(message.getAckStatus())
                            .createdAt(message.getCreatedAt())
                            .build());
                }

                // 标记MySQL中的离线消息为已推送
                OfflineMessage offline = offlineMessageRepository.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OfflineMessage>()
                                .eq(OfflineMessage::getMessageId, messageId)
                                .eq(OfflineMessage::getReceiverId, userId));
                if (offline != null) {
                    offline.setIsPushed(1);
                    offline.setPushedAt(LocalDateTime.now());
                    offlineMessageRepository.updateById(offline);
                }
            } catch (Exception e) {
                log.error("处理离线消息失败: messageId={}", msgId, e);
            }
        }

        // 清理Redis中的离线消息
        redisUtils.deleteList(offlineKey);

        log.info("离线消息已推送: userId={}, 消息数={}", userId, responses.size());
        return responses;
    }

    @Override
    public void pushOfflineMessages(Long userId) {
        getAndClearOfflineMessages(userId);
    }
}