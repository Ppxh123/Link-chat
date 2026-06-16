package com.linkchat.server.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateGroupAnnouncementRequest {
    @Size(max = 500, message = "公告最长500个字符")
    private String announcement;
}