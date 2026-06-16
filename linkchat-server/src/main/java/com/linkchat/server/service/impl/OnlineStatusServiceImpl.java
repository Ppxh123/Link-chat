package com.linkchat.server.service.impl;

import com.linkchat.server.common.Constants;
import com.linkchat.server.entity.User;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.service.OnlineStatusService;
import com.linkchat.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private final RedisUtils redisUtils;
    private final UserRepository userRepository;

    @Override
    public void updateStatus(Long userId, Integer status) {
        String key = Constants.REDIS_ONLINE_STATUS + userId;
        redisUtils.set(key, String.valueOf(status));
    }

    @Override
    public Integer getStatus(Long userId) {
        String key = Constants.REDIS_ONLINE_STATUS + userId;
        Object value = redisUtils.get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return Constants.STATUS_OFFLINE;
    }

    @Override
    public void userOnline(Long userId) {
        // 更新Redis在线状态
        String sessionKey = Constants.REDIS_ONLINE_SESSION + userId;
        redisUtils.set(sessionKey, "1", Duration.ofSeconds(120).getSeconds(), TimeUnit.SECONDS);

        updateStatus(userId, Constants.STATUS_ONLINE);

        // 更新MySQL
        User user = userRepository.selectById(userId);
        if (user != null) {
            user.setStatus(Constants.STATUS_ONLINE);
            userRepository.updateById(user);
        }

        log.info("用户上线: userId={}", userId);
    }

    @Override
    public void userOffline(Long userId) {
        // 清除Redis
        String sessionKey = Constants.REDIS_ONLINE_SESSION + userId;
        redisUtils.delete(sessionKey);

        updateStatus(userId, Constants.STATUS_OFFLINE);

        // 更新MySQL
        User user = userRepository.selectById(userId);
        if (user != null) {
            user.setStatus(Constants.STATUS_OFFLINE);
            user.setLastOnline(LocalDateTime.now());
            userRepository.updateById(user);
        }

        log.info("用户离线: userId={}", userId);
    }
}