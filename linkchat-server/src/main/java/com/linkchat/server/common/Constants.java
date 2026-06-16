package com.linkchat.server.common;

public interface Constants {

    // 消息类型
    String MSG_TYPE_TEXT = "TEXT";
    String MSG_TYPE_IMAGE = "IMAGE";
    String MSG_TYPE_FILE = "FILE";
    String MSG_TYPE_EMOJI = "EMOJI";
    String MSG_TYPE_SYSTEM = "SYSTEM";

    // ACK状态
    String ACK_SENT = "SENT";
    String ACK_DELIVERED = "DELIVERED";
    String ACK_READ = "READ";
    String ACK_FAILED = "FAILED";

    // 好友状态
    int FRIEND_PENDING = 0;
    int FRIEND_ACCEPTED = 1;
    int FRIEND_REJECTED = 2;
    int FRIEND_DELETED = 3;

    // 在线状态
    int STATUS_OFFLINE = 0;
    int STATUS_ONLINE = 1;
    int STATUS_BUSY = 2;
    int STATUS_INVISIBLE = 3;

    // 群成员角色
    int ROLE_OWNER = 0;
    int ROLE_ADMIN = 1;
    int ROLE_MEMBER = 2;

    // ACK类型
    int ACK_TYPE_DELIVERED = 1;
    int ACK_TYPE_READ = 2;

    // 消息重试
    int MAX_RETRY_COUNT = 3;
    long ACK_TIMEOUT_MS = 5_000;

    // 消息撤回时限
    long RECALL_TIMEOUT_MS = 120_000;

    // 心跳
    long HEARTBEAT_TIMEOUT_MS = 90_000;

    // 分页
    int DEFAULT_PAGE_SIZE = 20;

    // 文件大小限制
    long MAX_FILE_SIZE = 50 * 1024 * 1024;

    // 允许的文件类型
    String[] ALLOWED_FILE_TYPES = {
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "zip", "rar", "7z", "csv", "md", "json", "xml"
    };

    // Redis Key前缀
    String REDIS_TOKEN_PREFIX = "token:";
    String REDIS_TOKEN_VERSION = "token:version:";
    String REDIS_ONLINE_STATUS = "online:status:";
    String REDIS_ONLINE_SESSION = "online:session:";
    String REDIS_OFFLINE_MSG = "offline:msg:";
    String REDIS_MSG_ACK = "msg:ack:";
    String REDIS_MSG_DEDUP = "msg:dedup:";
    String REDIS_TYPING = "typing:";
    String REDIS_RATELIMIT = "ratelimit:";
}