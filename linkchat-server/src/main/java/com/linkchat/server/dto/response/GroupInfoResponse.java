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
public class GroupInfoResponse {
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long ownerId;
    private String name;
    private String avatarUrl;
    private String announcement;
    private Integer memberCount;
    private Integer myRole;
    private Integer isMuted;
    private Integer ownerIsMuted;
    private LocalDateTime createdAt;
}