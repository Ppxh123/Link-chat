package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.dto.request.SendMessageRequest;
import com.linkchat.server.dto.response.MessageResponse;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtils securityUtils;

    @PostMapping("/send")
    public Result<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(messageService.sendMessage(userId, request));
    }

    @GetMapping("/history")
    public Result<List<MessageResponse>> getChatHistory(
            @RequestParam(required = false) Long peerId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(messageService.getChatHistory(userId, peerId, groupId, page, size));
    }

    @PutMapping("/recall/{messageId}")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        Long userId = securityUtils.getCurrentUserId();
        messageService.recallMessage(userId, messageId);
        return Result.success();
    }

    @DeleteMapping("/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        Long userId = securityUtils.getCurrentUserId();
        messageService.deleteMessage(userId, messageId);
        return Result.success();
    }

    @PostMapping("/forward/{messageId}")
    public Result<MessageResponse> forwardMessage(@PathVariable Long messageId, @RequestParam Long targetId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(messageService.forwardMessage(userId, messageId, targetId));
    }

    @GetMapping("/search")
    public Result<List<MessageResponse>> searchMessages(@RequestParam Long peerId, @RequestParam String keyword) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(messageService.searchMessages(userId, peerId, keyword));
    }

    @PutMapping("/read/{senderId}")
    public Result<Void> markAsRead(@PathVariable Long senderId) {
        Long userId = securityUtils.getCurrentUserId();
        messageService.markAsRead(userId, senderId);
        return Result.success();
    }
}