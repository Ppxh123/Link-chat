package com.linkchat.server.websocket;

import com.linkchat.server.common.Constants;
import com.linkchat.server.entity.Message;
import com.linkchat.server.entity.MessageAck;
import com.linkchat.server.repository.MessageAckRepository;
import com.linkchat.server.repository.MessageRepository;
import com.linkchat.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageAckHandler {

    private final MessageRepository messageRepository;
    private final MessageAckRepository messageAckRepository;
    private final RedisUtils redisUtils;

    public void handleDelivered(Long userId, Long messageId) {
        // 更新Redis ACK状态
        String ackKey = Constants.REDIS_MSG_ACK + messageId;
        redisUtils.set(ackKey, Constants.ACK_DELIVERED, 1, TimeUnit.HOURS);

        // 更新MySQL
        Message message = messageRepository.selectById(messageId);
        if (message != null && Constants.ACK_SENT.equals(message.getAckStatus())) {
            message.setAckStatus(Constants.ACK_DELIVERED);
            messageRepository.updateById(message);
        }

        // 记录ACK
        MessageAck ack = new MessageAck();
        ack.setMessageId(messageId);
        ack.setUserId(userId);
        ack.setAckType(Constants.ACK_TYPE_DELIVERED);
        ack.setAckTime(LocalDateTime.now());
        messageAckRepository.insert(ack);

        log.debug("消息已送达: messageId={}, userId={}", messageId, userId);
    }

    public void handleRead(Long userId, Long messageId) {
        String ackKey = Constants.REDIS_MSG_ACK + messageId;
        redisUtils.set(ackKey, Constants.ACK_READ, 1, TimeUnit.HOURS);

        Message message = messageRepository.selectById(messageId);
        if (message != null) {
            message.setAckStatus(Constants.ACK_READ);
            messageRepository.updateById(message);
        }

        MessageAck ack = new MessageAck();
        ack.setMessageId(messageId);
        ack.setUserId(userId);
        ack.setAckType(Constants.ACK_TYPE_READ);
        ack.setAckTime(LocalDateTime.now());
        messageAckRepository.insert(ack);

        log.debug("消息已读: messageId={}, userId={}", messageId, userId);
    }
}