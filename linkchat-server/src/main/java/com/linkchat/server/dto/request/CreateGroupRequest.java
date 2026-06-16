package com.linkchat.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "群名称不能为空")
    @Size(max = 30, message = "群名称最长30个字符")
    private String name;

    @NotEmpty(message = "至少选择一个好友")
    private List<Long> memberIds;
}