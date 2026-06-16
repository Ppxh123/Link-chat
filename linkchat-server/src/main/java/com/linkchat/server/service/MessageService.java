package com.linkchat.server.service;

import com.linkchat.server.dto.request.SendMessageRequest;
import com.linkchat.server.dto.response.MessageResponse;
import com.linkchat.server.entity.Message;

import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(Long senderId, SendMessageRequest request);
    List<MessageResponse> getChatHistory(Long userId, Long peerId, Long groupId, int page, int size);
    void recallMessage(Long userId, Long messageId);
    void deleteMessage(Long userId, Long messageId);
    MessageResponse forwardMessage(Long userId, Long messageId, Long targetId);
    List<MessageResponse> searchMessages(Long userId, Long peerId, String keyword);
    void markAsRead(Long userId, Long senderId);
    MessageResponse toMessageResponse(Message message);
}