package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/status")
@RequiredArgsConstructor
public class OnlineStatusController {

    private final OnlineStatusService onlineStatusService;
    private final SecurityUtils securityUtils;

    @PutMapping
    public Result<Void> updateStatus(@RequestParam Integer status) {
        Long userId = securityUtils.getCurrentUserId();
        onlineStatusService.updateStatus(userId, status);
        return Result.success();
    }

    @GetMapping("/{userId}")
    public Result<Integer> getStatus(@PathVariable Long userId) {
        return Result.success(onlineStatusService.getStatus(userId));
    }
}