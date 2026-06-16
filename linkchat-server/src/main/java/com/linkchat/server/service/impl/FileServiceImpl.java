package com.linkchat.server.service.impl;

import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.Constants;
import com.linkchat.server.common.ResultCode;
import com.linkchat.server.entity.FileRecord;
import com.linkchat.server.repository.FileRecordRepository;
import com.linkchat.server.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final FileRecordRepository fileRecordRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public Map<String, String> uploadFile(Long uploaderId, MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String objectName = "files/" + UUID.randomUUID().toString() + "." + extension;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String fileUrl = endpoint + "/" + bucketName + "/" + objectName;

            FileRecord record = new FileRecord();
            record.setUploaderId(uploaderId);
            record.setFileName(originalName);
            record.setFileUrl(fileUrl);
            record.setFileSize(file.getSize());
            record.setFileType(extension.toLowerCase());
            record.setMimeType(file.getContentType());
            fileRecordRepository.insert(record);

            Map<String, String> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", originalName);
            result.put("fileSize", String.valueOf(file.getSize()));
            result.put("fileType", extension.toLowerCase());
            return result;

        } catch (Exception e) {
            log.error("文件上传失败: fileName={}, 错误: {}", originalName, e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED.getCode(),
                        "文件存储服务(MinIO)未启动，请联系管理员");
            }
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public Map<String, String> uploadAvatar(Long uploaderId, MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String objectName = "avatars/" + uploaderId + "_" + UUID.randomUUID().toString() + "." + extension;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String fileUrl = endpoint + "/" + bucketName + "/" + objectName;

            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", fileUrl);
            return result;

        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED.getCode(),
                        "文件存储服务(MinIO)未启动，请联系管理员");
            }
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > Constants.MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!Arrays.asList(Constants.ALLOWED_FILE_TYPES).contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
    }

    private String getExtension(String filename) {
        if (filename == null)
            return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }
}