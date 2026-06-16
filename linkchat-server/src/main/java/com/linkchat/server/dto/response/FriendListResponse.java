package com.linkchat.server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendListResponse {
    private Long friendId;
    private String nickname;
    private String userCode;
    private String avatarUrl;
    private String signature;
    private String remark;
    private Integer status;
    private LocalDateTime lastOnline;
}