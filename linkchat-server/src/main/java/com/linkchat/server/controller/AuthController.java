package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.dto.request.ChangePasswordRequest;
import com.linkchat.server.dto.request.LoginRequest;
import com.linkchat.server.dto.request.RegisterRequest;
import com.linkchat.server.dto.response.LoginResponse;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        authService.changePassword(userId, request);
        return Result.success();
    }
}