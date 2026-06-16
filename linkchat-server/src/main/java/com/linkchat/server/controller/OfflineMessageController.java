package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.dto.response.MessageResponse;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.OfflineMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/offline")
@RequiredArgsConstructor
public class OfflineMessageController {

    private final OfflineMessageService offlineMessageService;
    private final SecurityUtils securityUtils;

    @GetMapping("/messages")
    public Result<List<MessageResponse>> getOfflineMessages() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(offlineMessageService.getAndClearOfflineMessages(userId));
    }
}