package com.linkchat.server.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkchat.server.common.Constants;
import com.linkchat.server.entity.GroupMember;
import com.linkchat.server.entity.Message;
import com.linkchat.server.repository.GroupMemberRepository;
import com.linkchat.server.repository.MessageRepository;
import com.linkchat.server.security.JwtTokenProvider;
import com.linkchat.server.service.MessageService;
import com.linkchat.server.service.OnlineStatusService;
import com.linkchat.server.util.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WebSocket 服务端
 * 负责连接管理、消息转发、心跳检测、ACK确认
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{token}")
public class WebSocketServer {

    /** userId -> Session 映射 */
    private static final ConcurrentHashMap<Long, Session> sessionMap = new ConcurrentHashMap<>();

    private static JwtTokenProvider jwtTokenProvider;
    private static OnlineStatusService onlineStatusService;
    private static RedisUtils redisUtils;
    private static MessageService messageService;
    private static ObjectMapper objectMapper;
    private static GroupMemberRepository groupMemberRepository;
    private static MessageRepository messageRepository;

    @Autowired
    public void setJwtTokenProvider(JwtTokenProvider jwtTokenProvider) {
        WebSocketServer.jwtTokenProvider = jwtTokenProvider;
    }

    @Autowired
    public void setOnlineStatusService(OnlineStatusService onlineStatusService) {
        WebSocketServer.onlineStatusService = onlineStatusService;
    }

    @Autowired
    public void setRedisUtils(RedisUtils redisUtils) {
        WebSocketServer.redisUtils = redisUtils;
    }

    @Autowired
    public void setMessageService(MessageService messageService) {
        WebSocketServer.messageService = messageService;
    }

    @Autowired
    public void setGroupMemberRepository(GroupMemberRepository groupMemberRepository) {
        WebSocketServer.groupMemberRepository = groupMemberRepository;
    }

    @Autowired
    public void setMessageRepository(MessageRepository messageRepository) {
        WebSocketServer.messageRepository = messageRepository;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        WebSocketServer.objectMapper = objectMapper;
    }

    /**
     * WebSocket 连接建立
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Token无效"));
                log.warn("WebSocket认证失败: token无效");
                return;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            sessionMap.put(userId, session);
            HeartbeatManager.updateHeartbeat(userId);

            // 更新在线状态
            onlineStatusService.userOnline(userId);
            String sessionKey = Constants.REDIS_ONLINE_SESSION + userId;
            redisUtils.set(sessionKey, "1", Duration.ofSeconds(120).getSeconds(), TimeUnit.SECONDS);

            log.info("WebSocket连接建立: userId={}", userId);

            // 推送离线消息
            pushOfflineMessages(userId, session);

        } catch (Exception e) {
            log.error("WebSocket onOpen异常", e);
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * WebSocket 连接关闭
     */
    @OnClose
    public void onClose(Session session) {
        Long userId = getUserIdBySession(session);
        if (userId != null) {
            sessionMap.remove(userId);
            HeartbeatManager.removeHeartbeat(userId);
            onlineStatusService.userOffline(userId);
            log.info("WebSocket连接关闭: userId={}", userId);
        }
    }

    /**
     * WebSocket 错误处理
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误", error);
        Long userId = getUserIdBySession(session);
        if (userId != null) {
            sessionMap.remove(userId);
            HeartbeatManager.removeHeartbeat(userId);
            onlineStatusService.userOffline(userId);
        }
    }

    /**
     * 消息处理入口 - 根据type分发到不同的Handler
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(message);
            String type = jsonObject.getStr("type");
            Long userId = getUserIdBySession(session);
            if (userId == null)
                return;

            switch (type) {
                case "ping" -> handlePing(userId, session);
                case "ack" -> handleAck(userId, jsonObject);
                case "typing" -> handleTyping(userId, jsonObject);
                case "message" -> handleMessage(userId, jsonObject);
                case "recall" -> handleRecall(userId, jsonObject);
                default -> log.debug("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("WebSocket消息处理异常", e);
        }
    }

    // ==================== 消息处理器 ====================

    /**
     * 心跳PING处理
     */
    private void handlePing(Long userId, Session session) {
        HeartbeatManager.updateHeartbeat(userId);
        sendMessage(session, "{\"type\":\"pong\"}");

        // 心跳续期Redis在线状态
        String sessionKey = Constants.REDIS_ONLINE_SESSION + userId;
        redisUtils.set(sessionKey, "1", Duration.ofSeconds(120).getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * ACK确认处理 — 使用 Jackson 序列化确保 messageId 字符串格式
     */
    private void handleAck(Long userId, JSONObject jsonObject) {
        JSONObject payload = jsonObject.getJSONObject("payload");
        if (payload == null)
            return;

        Long messageId = payload.getLong("messageId");
        String ackType = payload.getStr("ackType", "DELIVERED");

        // 更新Redis中的消息ACK状态
        String ackKey = Constants.REDIS_MSG_ACK + messageId;
        redisUtils.set(ackKey, ackType, 30, TimeUnit.MINUTES);

        // 通知消息发送者
        Long senderId = payload.getLong("senderId");
        if (senderId != null) {
            Session senderSession = sessionMap.get(senderId);
            if (senderSession != null) {
                Map<String, Object> ackPayload = new HashMap<>();
                ackPayload.put("messageId", String.valueOf(messageId));
                ackPayload.put("status", "READ".equals(ackType) ? Constants.ACK_READ : Constants.ACK_DELIVERED);
                sendMessage(senderSession, buildWsMessage("ack", ackPayload));
            }
        }

        // 同时更新数据库消息ACK状态
        try {
            messageService.markAsRead(userId, senderId);
        } catch (Exception e) {
            log.error("更新消息ACK状态失败: messageId={}", messageId, e);
        }
    }

    /**
     * 对方正在输入...处理 — 使用 Jackson 序列化确保 senderId 字符串格式
     */
    private void handleTyping(Long userId, JSONObject jsonObject) {
        JSONObject payload = jsonObject.getJSONObject("payload");
        if (payload == null)
            return;

        Long receiverId = payload.getLong("receiverId");
        Boolean active = payload.getBool("active");
        boolean isActive = active == null || active;

        log.info("收到typing事件: senderId={}, receiverId={}, active={}", userId, receiverId, isActive);
        if (receiverId != null) {
            Session receiverSession = sessionMap.get(receiverId);
            if (receiverSession != null) {
                log.info("typing已转发: senderId={} → receiverId={}", userId, receiverId);
                Map<String, Object> typingPayload = new HashMap<>();
                typingPayload.put("senderId", String.valueOf(userId));
                typingPayload.put("active", isActive);
                sendMessage(receiverSession, buildWsMessage("typing", typingPayload));
            } else {
                log.info("typing转发失败，接收者不在线: receiverId={}", receiverId);
            }
        }
    }

    /**
     * 消息转发 — 使用 Jackson 序列化，确保 Long 字段以字符串格式传输
     */
    private void handleMessage(Long userId, JSONObject jsonObject) {
        JSONObject payload = jsonObject.getJSONObject("payload");
        if (payload == null)
            return;

        Long receiverId = payload.getLong("receiverId");
        Long groupId = payload.getLong("groupId");

        if (groupId != null) {
            // 群聊消息：广播给群内所有在线成员
            broadcastGroupMessage(groupId, buildWsMessage("message", payload), userId);

            // 广播 stop-typing，确保群成员清除发送者的"正在输入"指示器
            Map<String, Object> stopTyping = new HashMap<>();
            stopTyping.put("senderId", String.valueOf(userId));
            stopTyping.put("active", false);
            broadcastGroupMessage(groupId, buildWsMessage("typing", stopTyping), userId);
        } else if (receiverId != null) {
            // 单聊消息：发送给目标用户
            Session receiverSession = sessionMap.get(receiverId);
            if (receiverSession != null) {
                sendMessage(receiverSession, buildWsMessage("message", payload));

                // 同时发送 stop-typing，确保接收方清除"对方正在输入"指示器
                Map<String, Object> stopTyping = new HashMap<>();
                stopTyping.put("senderId", String.valueOf(userId));
                stopTyping.put("active", false);
                sendMessage(receiverSession, buildWsMessage("typing", stopTyping));
            } else {
                log.debug("接收者不在线: receiverId={}", receiverId);
            }
        }
    }

    /**
     * 消息撤回 — 使用 Jackson 序列化确保 messageId 字符串格式
     */
    private void handleRecall(Long userId, JSONObject jsonObject) {
        JSONObject payload = jsonObject.getJSONObject("payload");
        if (payload == null)
            return;

        String messageId = payload.getStr("messageId");
        // 查询消息获取接收者和群信息
        Message message = messageRepository.selectById(Long.valueOf(messageId));
        if (message == null) return;

        Map<String, Object> recallPayload = Map.of("messageId", messageId, "senderId", String.valueOf(userId));
        String recallMsg = buildWsMessage("recall", recallPayload);

        if (message.getGroupId() != null) {
            // 群聊消息：通知群内所有在线成员
            broadcastGroupMessage(message.getGroupId(), recallMsg, userId);
            // 也发给发送者自己（群聊中发送者也要看到撤回提示）
            Session senderSession = sessionMap.get(userId);
            if (senderSession != null) {
                sendMessage(senderSession, recallMsg);
            }
        } else {
            // 单聊消息：只通知发送者和接收者
            Session senderSession = sessionMap.get(userId);
            if (senderSession != null) {
                sendMessage(senderSession, recallMsg);
            }
            if (message.getReceiverId() != null) {
                Session receiverSession = sessionMap.get(message.getReceiverId());
                if (receiverSession != null) {
                    sendMessage(receiverSession, recallMsg);
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 推送离线消息 — 使用 Jackson 序列化确保 Long 字段字符串格式
     */
    private void pushOfflineMessages(Long userId, Session session) {
        String offlineKey = Constants.REDIS_OFFLINE_MSG + userId;
        List<Object> msgIds = redisUtils.range(offlineKey, 0, -1);
        if (msgIds != null && !msgIds.isEmpty()) {
            log.info("推送 {} 条离线消息给 userId={}", msgIds.size(), userId);
            for (Object msgIdObj : msgIds) {
                try {
                    Long msgId = Long.valueOf(msgIdObj.toString());
                    Message msg = messageRepository.selectById(msgId);
                    if (msg != null) {
                        sendMessage(session, buildWsMessage("message", messageService.toMessageResponse(msg)));
                    }
                } catch (Exception e) {
                    log.error("推送离线消息失败: msgId={}", msgIdObj, e);
                }
            }
            redisUtils.delete(offlineKey);
        }
    }

    /**
     * 公开的群聊广播方法 — 供 MessageService 等服务调用
     */
    public static void broadcastToGroup(Long groupId, String message, Long excludeUserId) {
        List<GroupMember> members = groupMemberRepository.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));
        Set<Long> memberIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toCollection(java.util.HashSet::new));
        log.info("broadcastToGroup: groupId={}, members={}, onlineSessions={}",
                groupId, memberIds.size(), sessionMap.size());
        log.info("broadcastToGroup: online userIds={}", sessionMap.keySet());

        sessionMap.forEach((userId, session) -> {
            if (memberIds.contains(userId) && !userId.equals(excludeUserId)) {
                sendMessage(session, message);
            }
        });
    }

    /**
     * 群发消息广播 — 仅广播给群内在线成员
     */
    private void broadcastGroupMessage(Long groupId, String message, Long excludeUserId) {
        // 查询群成员列表
        List<GroupMember> members = groupMemberRepository.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));
        Set<Long> memberIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toSet());

        sessionMap.forEach((userId, session) -> {
            if (memberIds.contains(userId) && !userId.equals(excludeUserId)) {
                sendMessage(session, message);
            }
        });
    }

    // ==================== 静态工具方法 ====================

    /**
     * 向指定Session发送消息 — 使用异步发送，支持跨线程调用
     */
    public static void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message, result -> {
                    if (!result.isOK()) {
                        log.error("WebSocket异步发送失败: {}", result.getException() != null ? result.getException().getMessage() : "未知错误");
                    }
                });
            } catch (Exception e) {
                log.error("WebSocket发送消息失败", e);
            }
        }
    }

    /**
     * 根据userId获取Session
     */
    public static Session getSession(Long userId) {
        return sessionMap.get(userId);
    }

    /**
     * 判断用户是否在线
     */
    public static boolean isOnline(Long userId) {
        Session session = sessionMap.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线用户数
     */
    public static int getOnlineCount() {
        return sessionMap.size();
    }

    /**
     * 根据Session反查userId
     */
    private Long getUserIdBySession(Session session) {
        for (var entry : sessionMap.entrySet()) {
            if (entry.getValue().getId().equals(session.getId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 使用 Jackson 构建 WebSocket 消息 JSON，确保 Long 字段序列化为字符串
     */
    private static String buildWsMessage(String type, Object payload) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", type);
            msg.put("payload", payload);
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            log.error("构建WebSocket消息失败: type={}", type, e);
            return "{}";
        }
    }
}