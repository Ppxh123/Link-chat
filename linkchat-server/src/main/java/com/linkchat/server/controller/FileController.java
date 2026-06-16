package com.linkchat.server.controller;

import com.linkchat.server.common.Result;
import com.linkchat.server.security.SecurityUtils;
import com.linkchat.server.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final SecurityUtils securityUtils;

    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(fileService.uploadFile(userId, file));
    }
}