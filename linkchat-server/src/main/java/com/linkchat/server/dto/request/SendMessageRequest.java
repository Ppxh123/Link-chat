package com.linkchat.server.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    private Long receiverId;

    private Long groupId;

    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    private String content;

    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMime;

    private Long quotedMsgId;

    /**
     * 单聊和群聊至少指定一个目标
     */
    @AssertTrue(message = "单聊需指定receiverId，群聊需指定groupId")
    public boolean isValidTarget() {
        return receiverId != null || groupId != null;
    }
}