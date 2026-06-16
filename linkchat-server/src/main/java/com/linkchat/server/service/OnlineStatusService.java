package com.linkchat.server.service;

public interface OnlineStatusService {
    void updateStatus(Long userId, Integer status);
    Integer getStatus(Long userId);
    void userOnline(Long userId);
    void userOffline(Long userId);
}