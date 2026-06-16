package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;
    private Integer isMuted;
    private String nicknameInGroup;
    private LocalDateTime joinedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}