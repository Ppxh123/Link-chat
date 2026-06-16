package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("online_user")
public class OnlineUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer status;
    private String serverNode;
    private String clientIp;
    private LocalDateTime heartbeatAt;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
}