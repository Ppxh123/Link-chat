package com.linkchat.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploaderId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private Long messageId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}