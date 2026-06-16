package com.linkchat.server.service;

import com.linkchat.server.dto.request.ChangePasswordRequest;
import com.linkchat.server.dto.request.LoginRequest;
import com.linkchat.server.dto.request.RegisterRequest;
import com.linkchat.server.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}