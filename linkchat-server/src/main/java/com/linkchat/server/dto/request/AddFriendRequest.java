package com.linkchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddFriendRequest {
    @NotBlank(message = "好友标识不能为空")
    private String keyword;
}