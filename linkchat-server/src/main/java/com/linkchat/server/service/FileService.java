package com.linkchat.server.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {
    Map<String, String> uploadFile(Long uploaderId, MultipartFile file);
    Map<String, String> uploadAvatar(Long uploaderId, MultipartFile file);
}