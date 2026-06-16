package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private String messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMime;
    private Integer isRecalled;
    private Integer isDeleted;
    private Long quotedMsgId;
    private String ackStatus;
    private Integer retryCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}