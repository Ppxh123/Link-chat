package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_chat")
public class GroupChat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatarUrl;
    private Long ownerId;
    private String announcement;
    private Integer isMuted;
    private Integer memberCount;
    @TableLogic
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}