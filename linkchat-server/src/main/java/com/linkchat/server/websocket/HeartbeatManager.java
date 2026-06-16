package com.linkchat.server.websocket;

import com.linkchat.server.common.Constants;
import com.linkchat.server.service.OnlineStatusService;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class HeartbeatManager {

    private final OnlineStatusService onlineStatusService;

    private static ConcurrentHashMap<Long, Long> lastHeartbeat = new ConcurrentHashMap<>();

    public static void updateHeartbeat(Long userId) {
        lastHeartbeat.put(userId, System.currentTimeMillis());
    }

    public static void removeHeartbeat(Long userId) {
        lastHeartbeat.remove(userId);
    }

    @Scheduled(fixedRate = 30000)
    public void checkHeartbeat() {
        long now = System.currentTimeMillis();
        // 先收集超时的userId，避免在遍历中直接remove导致并发问题
        List<Long> timeoutUsers = new ArrayList<>();
        lastHeartbeat.forEach((userId, lastTime) -> {
            if (now - lastTime > Constants.HEARTBEAT_TIMEOUT_MS) {
                timeoutUsers.add(userId);
            }
        });

        for (Long userId : timeoutUsers) {
            Session session = WebSocketServer.getSession(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                } catch (Exception e) {
                    log.error("关闭超时连接失败: userId={}", userId);
                }
            }
            lastHeartbeat.remove(userId);
            onlineStatusService.userOffline(userId);
            log.info("心跳超时断开: userId={}", userId);
        }
    }
}