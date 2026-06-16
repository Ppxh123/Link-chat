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
public class UserInfoResponse {
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long id;
    private String email;
    private String nickname;
    private String userCode;
    private String avatarUrl;
    private String signature;
    private Integer role;
    private Integer isMuted;
    private Integer status;
    private LocalDateTime lastOnline;
}