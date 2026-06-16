-- ============================================
-- LinkChat 数据库初始化脚本
-- Database: linkchat
-- Charset: utf8mb4
-- Engine: InnoDB
-- Author: LinkChat Team
-- Date: 2026-06-11
-- ============================================

CREATE DATABASE IF NOT EXISTS linkchat
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE linkchat;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email`       VARCHAR(128) NOT NULL COMMENT '邮箱（登录账号）',
    `password`    VARCHAR(256) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`    VARCHAR(64)  NOT NULL COMMENT '昵称',
    `user_code`   VARCHAR(32)  NOT NULL COMMENT '用户唯一码（10位数字，可搜索加好友）',
    `avatar_url`  VARCHAR(512)          DEFAULT NULL COMMENT '头像URL（MinIO）',
    `signature`   VARCHAR(256)          DEFAULT NULL COMMENT '个性签名',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '在线状态快照 0=离线 1=在线 2=忙碌 3=隐身',
    `last_online` DATETIME              DEFAULT NULL COMMENT '最后在线时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=已注销',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_user_code` (`user_code`),
    INDEX `idx_nickname` (`nickname`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- ============================================
-- 2. 好友关系表
-- ============================================
CREATE TABLE IF NOT EXISTS `friend`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '好友关系ID',
    `user_id`    BIGINT      NOT NULL COMMENT '用户ID',
    `friend_id`  BIGINT      NOT NULL COMMENT '好友ID',
    `status`     TINYINT     NOT NULL DEFAULT 0 COMMENT '0=待同意 1=已同意 2=已拒绝 3=已删除',
    `remark`     VARCHAR(64)          DEFAULT NULL COMMENT '好友备注名',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_friend_id` (`friend_id`),
    CONSTRAINT `fk_friend_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_friend_friend` FOREIGN KEY (`friend_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='好友关系表';

-- ============================================
-- 3. 群聊表
-- ============================================
CREATE TABLE IF NOT EXISTS `group_chat`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '群ID',
    `name`          VARCHAR(128) NOT NULL COMMENT '群名称',
    `avatar_url`    VARCHAR(512)          DEFAULT NULL COMMENT '群头像URL',
    `owner_id`      BIGINT       NOT NULL COMMENT '群主ID',
    `announcement`  TEXT                  DEFAULT NULL COMMENT '群公告',
    `member_count`  INT          NOT NULL DEFAULT 1 COMMENT '成员数量',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常 1=已解散',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_owner` (`owner_id`),
    INDEX `idx_name` (`name`),
    CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='群聊表';

-- ============================================
-- 4. 群成员表
-- ============================================
CREATE TABLE IF NOT EXISTS `group_member`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
    `group_id`           BIGINT      NOT NULL COMMENT '群ID',
    `user_id`            BIGINT      NOT NULL COMMENT '用户ID',
    `role`               TINYINT     NOT NULL DEFAULT 2 COMMENT '0=OWNER 1=ADMIN 2=MEMBER',
    `nickname_in_group`  VARCHAR(64)          DEFAULT NULL COMMENT '群内昵称',
    `joined_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_user_role` (`user_id`, `role`),
    CONSTRAINT `fk_gm_group` FOREIGN KEY (`group_id`) REFERENCES `group_chat` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='群成员表';

-- ============================================
-- 5. 消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `message`
(
    `id`            BIGINT       NOT NULL COMMENT '消息ID（雪花算法生成）',
    `sender_id`     BIGINT       NOT NULL COMMENT '发送者ID',
    `receiver_id`   BIGINT                DEFAULT NULL COMMENT '接收者ID（单聊用，群聊为NULL）',
    `group_id`      BIGINT                DEFAULT NULL COMMENT '群ID（群聊用，单聊为NULL）',
    `message_type`  VARCHAR(16)  NOT NULL COMMENT 'TEXT / IMAGE / FILE / EMOJI / SYSTEM',
    `content`       TEXT                  DEFAULT NULL COMMENT '消息文本内容',
    `file_url`      VARCHAR(512)          DEFAULT NULL COMMENT '文件URL（MinIO）',
    `file_name`     VARCHAR(256)          DEFAULT NULL COMMENT '原文件名',
    `file_size`     BIGINT                DEFAULT NULL COMMENT '文件大小（字节）',
    `file_mime`     VARCHAR(64)           DEFAULT NULL COMMENT 'MIME类型',
    `is_recalled`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否撤回 0=否 1=是',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0=否 1=是',
    `quoted_msg_id` BIGINT                DEFAULT NULL COMMENT '引用回复的消息ID',
    `ack_status`    VARCHAR(16)  NOT NULL DEFAULT 'SENT' COMMENT 'SENT / DELIVERED / READ / FAILED',
    `retry_count`   INT          NOT NULL DEFAULT 0 COMMENT '重试次数（最大3）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sender_receiver` (`sender_id`, `receiver_id`),
    INDEX `idx_group` (`group_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_quoted_msg` (`quoted_msg_id`),
    CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_msg_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_msg_group` FOREIGN KEY (`group_id`) REFERENCES `group_chat` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='消息表';

-- ============================================
-- 6. 消息ACK确认表
-- ============================================
CREATE TABLE IF NOT EXISTS `message_ack`
(
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ACK记录ID',
    `message_id` BIGINT   NOT NULL COMMENT '消息ID',
    `user_id`    BIGINT   NOT NULL COMMENT '确认用户ID',
    `ack_type`   TINYINT  NOT NULL COMMENT '1=DELIVERED（已送达） 2=READ（已读）',
    `ack_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '确认时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user_type` (`message_id`, `user_id`, `ack_type`),
    INDEX `idx_user_ack` (`user_id`, `ack_type`),
    CONSTRAINT `fk_ack_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ack_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='消息ACK确认表';

-- ============================================
-- 7. 离线消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `offline_message`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '离线消息ID',
    `message_id`   BIGINT       NOT NULL COMMENT '消息ID',
    `receiver_id`  BIGINT       NOT NULL COMMENT '接收者ID',
    `message_type` VARCHAR(16)  NOT NULL COMMENT '消息类型',
    `content`      TEXT                  DEFAULT NULL COMMENT '消息内容',
    `file_url`     VARCHAR(512)          DEFAULT NULL COMMENT '文件URL',
    `is_pushed`    TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未推送 1=已推送',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息原始发送时间',
    `pushed_at`    DATETIME              DEFAULT NULL COMMENT '推送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_receiver_pushed` (`receiver_id`, `is_pushed`),
    INDEX `idx_message_id` (`message_id`),
    CONSTRAINT `fk_offline_msg` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_offline_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='离线消息表';

-- ============================================
-- 8. 文件记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `file_record`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件记录ID',
    `uploader_id` BIGINT       NOT NULL COMMENT '上传者ID',
    `file_name`   VARCHAR(256) NOT NULL COMMENT '原始文件名',
    `file_url`    VARCHAR(512) NOT NULL COMMENT 'MinIO访问URL',
    `file_size`   BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `file_type`   VARCHAR(16)  NOT NULL COMMENT 'jpg / png / gif / pdf / zip / docx / pptx',
    `mime_type`   VARCHAR(64)  NOT NULL COMMENT 'MIME类型',
    `message_id`  BIGINT                DEFAULT NULL COMMENT '关联的消息ID',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    INDEX `idx_uploader` (`uploader_id`),
    INDEX `idx_message` (`message_id`),
    CONSTRAINT `fk_file_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件记录表';

-- ============================================
-- 9. 在线用户表（预留持久化统计）
-- ============================================
CREATE TABLE IF NOT EXISTS `online_user`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
    `status`          TINYINT     NOT NULL DEFAULT 0 COMMENT '0=离线 1=在线 2=忙碌 3=隐身',
    `server_node`     VARCHAR(64)          DEFAULT NULL COMMENT '服务器节点',
    `client_ip`       VARCHAR(45)          DEFAULT NULL COMMENT '客户端IP',
    `heartbeat_at`    DATETIME             DEFAULT NULL COMMENT '最后心跳时间',
    `connected_at`    DATETIME             DEFAULT NULL COMMENT '连接时间',
    `disconnected_at` DATETIME             DEFAULT NULL COMMENT '断开时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_online_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='在线用户表（预留）';

-- ============================================
-- 初始化完成
-- ============================================