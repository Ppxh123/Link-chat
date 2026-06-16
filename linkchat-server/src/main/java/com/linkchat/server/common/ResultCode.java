package com.linkchat.server.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_EMAIL_EXISTS(1003, "邮箱已注册"),
    USER_CODE_EXISTS(1004, "用户码已存在"),
    USER_OLD_PASSWORD_ERROR(1005, "原密码错误"),

    // 好友模块 2xxx
    FRIEND_ALREADY_EXISTS(2001, "已是好友"),
    FRIEND_REQUEST_EXISTS(2002, "已发送过好友申请"),
    FRIEND_NOT_FOUND(2003, "好友关系不存在"),
    FRIEND_SELF(2004, "不能添加自己为好友"),

    // 消息模块 3xxx
    MESSAGE_NOT_FOUND(3001, "消息不存在"),
    MESSAGE_RECALL_TIMEOUT(3002, "超过2分钟无法撤回"),
    MESSAGE_SEND_FAILED(3003, "消息发送失败"),
    MESSAGE_NOT_SENDER(3004, "只能撤回自己发送的消息"),
    MESSAGE_FORWARD_DENIED(3005, "无权转发此消息"),

    // 群聊模块 4xxx
    GROUP_NOT_FOUND(4001, "群聊不存在"),
    GROUP_NOT_MEMBER(4002, "不是群成员"),
    GROUP_NO_PERMISSION(4003, "无群管理权限"),
    GROUP_MEMBER_EXISTS(4004, "已在群中"),
    GROUP_OWNER_CANNOT_LEAVE(4005, "群主不能退群，请先转让群主"),
    GROUP_OWNER_CANNOT_BE_KICKED(4006, "不能踢出群主"),
    GROUP_ADMIN_CANNOT_BE_KICKED(4007, "管理员不能踢出其他管理员"),
    GROUP_MUTED(4008, "全员禁言中，仅群主和管理员可发言"),
    GROUP_MEMBER_MUTED(4009, "你已被禁言"),

    // 文件模块 5xxx
    FILE_TOO_LARGE(5001, "文件大小超过限制"),
    FILE_TYPE_NOT_SUPPORTED(5002, "不支持的文件类型"),
    FILE_UPLOAD_FAILED(5003, "文件上传失败"),
    FILE_NOT_FOUND(5004, "文件不存在"),

    // Token模块 6xxx
    TOKEN_EXPIRED(6001, "Token已过期"),
    TOKEN_INVALID(6002, "Token无效"),
    TOKEN_BLACKLISTED(6003, "Token已失效");

    private final int code;
    private final String message;
}
