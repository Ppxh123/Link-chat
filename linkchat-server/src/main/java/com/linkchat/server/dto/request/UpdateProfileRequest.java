package com.linkchat.server.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;

    @Size(max = 100, message = "签名最长100个字符")
    private String signature;
}