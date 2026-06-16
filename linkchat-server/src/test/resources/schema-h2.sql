-- ============================================
-- LinkChat H2测试数据库初始化脚本
-- 与MySQL schema保持一致，适配H2语法
-- ============================================

CREATE TABLE IF NOT EXISTS "user"
(
    "id"          BIGINT       NOT NULL AUTO_INCREMENT,
    "email"       VARCHAR(128) NOT NULL,
    "password"    VARCHAR(256) NOT NULL,
    "nickname"    VARCHAR(64)  NOT NULL,
    "user_code"   VARCHAR(32)  NOT NULL,
    "avatar_url"  VARCHAR(512)          DEFAULT NULL,
    "signature"   VARCHAR(256)          DEFAULT NULL,
    "status"      TINYINT      NOT NULL DEFAULT 0,
    "last_online" TIMESTAMP             DEFAULT NULL,
    "is_deleted"  TINYINT      NOT NULL DEFAULT 0,
    "created_at"  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at"  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "uk_email" UNIQUE ("email"),
    CONSTRAINT "uk_user_code" UNIQUE ("user_code")
);

CREATE INDEX "idx_user_nickname" ON "user" ("nickname");
CREATE INDEX "idx_user_status" ON "user" ("status");

CREATE TABLE IF NOT EXISTS "friend"
(
    "id"         BIGINT   NOT NULL AUTO_INCREMENT,
    "user_id"    BIGINT   NOT NULL,
    "friend_id"  BIGINT   NOT NULL,
    "status"     TINYINT  NOT NULL DEFAULT 0,
    "remark"     VARCHAR(64)        DEFAULT NULL,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "uk_user_friend" UNIQUE ("user_id", "friend_id"),
    CONSTRAINT "fk_friend_user" FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_friend_friend" FOREIGN KEY ("friend_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_friend_user_status" ON "friend" ("user_id", "status");
CREATE INDEX "idx_friend_friend_id" ON "friend" ("friend_id");

CREATE TABLE IF NOT EXISTS "group_chat"
(
    "id"            BIGINT       NOT NULL AUTO_INCREMENT,
    "name"          VARCHAR(128) NOT NULL,
    "avatar_url"    VARCHAR(512)          DEFAULT NULL,
    "owner_id"      BIGINT       NOT NULL,
    "announcement"  TEXT                  DEFAULT NULL,
    "member_count"  INT          NOT NULL DEFAULT 1,
    "is_deleted"    TINYINT      NOT NULL DEFAULT 0,
    "created_at"    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at"    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "fk_group_owner" FOREIGN KEY ("owner_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_group_owner" ON "group_chat" ("owner_id");
CREATE INDEX "idx_group_name" ON "group_chat" ("name");

CREATE TABLE IF NOT EXISTS "group_member"
(
    "id"                 BIGINT   NOT NULL AUTO_INCREMENT,
    "group_id"           BIGINT   NOT NULL,
    "user_id"            BIGINT   NOT NULL,
    "role"               TINYINT  NOT NULL DEFAULT 2,
    "nickname_in_group"  VARCHAR(64)       DEFAULT NULL,
    "joined_at"          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "created_at"         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "uk_group_user" UNIQUE ("group_id", "user_id"),
    CONSTRAINT "fk_gm_group" FOREIGN KEY ("group_id") REFERENCES "group_chat" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_gm_user" FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_gm_user_role" ON "group_member" ("user_id", "role");

CREATE TABLE IF NOT EXISTS "message"
(
    "id"            BIGINT      NOT NULL,
    "sender_id"     BIGINT      NOT NULL,
    "receiver_id"   BIGINT               DEFAULT NULL,
    "group_id"      BIGINT               DEFAULT NULL,
    "message_type"  VARCHAR(16) NOT NULL,
    "content"       TEXT                 DEFAULT NULL,
    "file_url"      VARCHAR(512)         DEFAULT NULL,
    "file_name"     VARCHAR(256)         DEFAULT NULL,
    "file_size"     BIGINT               DEFAULT NULL,
    "file_mime"     VARCHAR(64)          DEFAULT NULL,
    "is_recalled"   TINYINT     NOT NULL DEFAULT 0,
    "is_deleted"    TINYINT     NOT NULL DEFAULT 0,
    "quoted_msg_id" BIGINT               DEFAULT NULL,
    "ack_status"    VARCHAR(16) NOT NULL DEFAULT 'SENT',
    "retry_count"   INT         NOT NULL DEFAULT 0,
    "created_at"    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at"    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "fk_msg_sender" FOREIGN KEY ("sender_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_msg_receiver" FOREIGN KEY ("receiver_id") REFERENCES "user" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_msg_group" FOREIGN KEY ("group_id") REFERENCES "group_chat" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_msg_sender_receiver" ON "message" ("sender_id", "receiver_id");
CREATE INDEX "idx_msg_group" ON "message" ("group_id");
CREATE INDEX "idx_msg_created_at" ON "message" ("created_at");
CREATE INDEX "idx_msg_quoted" ON "message" ("quoted_msg_id");

CREATE TABLE IF NOT EXISTS "message_ack"
(
    "id"         BIGINT    NOT NULL AUTO_INCREMENT,
    "message_id" BIGINT    NOT NULL,
    "user_id"    BIGINT    NOT NULL,
    "ack_type"   TINYINT   NOT NULL,
    "ack_time"   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "uk_msg_user_type" UNIQUE ("message_id", "user_id", "ack_type"),
    CONSTRAINT "fk_ack_message" FOREIGN KEY ("message_id") REFERENCES "message" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_ack_user" FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_ack_user" ON "message_ack" ("user_id", "ack_type");

CREATE TABLE IF NOT EXISTS "offline_message"
(
    "id"           BIGINT      NOT NULL AUTO_INCREMENT,
    "message_id"   BIGINT      NOT NULL,
    "receiver_id"  BIGINT      NOT NULL,
    "message_type" VARCHAR(16) NOT NULL,
    "content"      TEXT                 DEFAULT NULL,
    "file_url"     VARCHAR(512)         DEFAULT NULL,
    "is_pushed"    TINYINT     NOT NULL DEFAULT 0,
    "created_at"   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "pushed_at"    TIMESTAMP            DEFAULT NULL,
    PRIMARY KEY ("id"),
    CONSTRAINT "fk_offline_msg" FOREIGN KEY ("message_id") REFERENCES "message" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_offline_receiver" FOREIGN KEY ("receiver_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_offline_receiver_pushed" ON "offline_message" ("receiver_id", "is_pushed");
CREATE INDEX "idx_offline_message_id" ON "offline_message" ("message_id");

CREATE TABLE IF NOT EXISTS "file_record"
(
    "id"          BIGINT       NOT NULL AUTO_INCREMENT,
    "uploader_id" BIGINT       NOT NULL,
    "file_name"   VARCHAR(256) NOT NULL,
    "file_url"    VARCHAR(512) NOT NULL,
    "file_size"   BIGINT       NOT NULL,
    "file_type"   VARCHAR(16)  NOT NULL,
    "mime_type"   VARCHAR(64)  NOT NULL,
    "message_id"  BIGINT                DEFAULT NULL,
    "created_at"  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "fk_file_uploader" FOREIGN KEY ("uploader_id") REFERENCES "user" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_file_uploader" ON "file_record" ("uploader_id");
CREATE INDEX "idx_file_message" ON "file_record" ("message_id");

CREATE TABLE IF NOT EXISTS "online_user"
(
    "id"              BIGINT   NOT NULL AUTO_INCREMENT,
    "user_id"         BIGINT   NOT NULL,
    "status"          TINYINT  NOT NULL DEFAULT 0,
    "server_node"     VARCHAR(64)       DEFAULT NULL,
    "client_ip"       VARCHAR(45)       DEFAULT NULL,
    "heartbeat_at"    TIMESTAMP         DEFAULT NULL,
    "connected_at"    TIMESTAMP         DEFAULT NULL,
    "disconnected_at" TIMESTAMP         DEFAULT NULL,
    PRIMARY KEY ("id"),
    CONSTRAINT "uk_online_user_id" UNIQUE ("user_id"),
    CONSTRAINT "fk_online_user" FOREIGN KEY ("user_id") REFERENCES "user" ("id") ON DELETE CASCADE
);