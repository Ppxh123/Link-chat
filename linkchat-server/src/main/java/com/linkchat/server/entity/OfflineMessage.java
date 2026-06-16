package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("offline_message")
public class OfflineMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long receiverId;
    private String messageType;
    private String content;
    private String fileUrl;
    private Integer isPushed;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime pushedAt;
}