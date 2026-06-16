package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message_ack")
public class MessageAck {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long userId;
    private Integer ackType;
    private LocalDateTime ackTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}