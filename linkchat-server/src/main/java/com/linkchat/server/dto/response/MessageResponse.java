package com.linkchat.server.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private Long groupId;
    private String messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMime;
    private Integer isRecalled;
    private Long quotedMsgId;
    private String ackStatus;
    private LocalDateTime createdAt;
}