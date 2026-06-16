-- Add is_muted columns for group chat

ALTER TABLE `group_chat`
    ADD COLUMN `is_muted` TINYINT NOT NULL DEFAULT 0 COMMENT '全员禁言 0=否 1=是' AFTER `member_count`;

ALTER TABLE `group_member`
    ADD COLUMN `is_muted` TINYINT NOT NULL DEFAULT 0 COMMENT '成员禁言 0=否 1=是' AFTER `role`;
