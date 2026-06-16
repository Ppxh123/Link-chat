package com.linkchat.server.service;

import com.linkchat.server.dto.response.MessageResponse;

import java.util.List;

public interface OfflineMessageService {
    void saveOfflineMessage(Long messageId, Long receiverId, String messageType, String content, String fileUrl);
    List<MessageResponse> getAndClearOfflineMessages(Long userId);
    void pushOfflineMessages(Long userId);
}