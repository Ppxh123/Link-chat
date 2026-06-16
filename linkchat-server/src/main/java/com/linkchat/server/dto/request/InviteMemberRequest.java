package com.linkchat.server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class InviteMemberRequest {
    @NotEmpty(message = "至少邀请一个成员")
    private List<Long> userIds;
}