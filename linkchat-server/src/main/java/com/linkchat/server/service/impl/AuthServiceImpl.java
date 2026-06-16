package com.linkchat.server.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.Constants;
import com.linkchat.server.common.ResultCode;
import com.linkchat.server.dto.request.ChangePasswordRequest;
import com.linkchat.server.dto.request.LoginRequest;
import com.linkchat.server.dto.request.RegisterRequest;
import com.linkchat.server.dto.response.LoginResponse;
import com.linkchat.server.entity.User;
import com.linkchat.server.repository.UserRepository;
import com.linkchat.server.security.JwtTokenProvider;
import com.linkchat.server.service.AuthService;
import com.linkchat.server.util.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtils redisUtils;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 检查邮箱是否已注册
        if (userRepository.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()))) {
            throw new BusinessException(ResultCode.USER_EMAIL_EXISTS);
        }

        // 生成唯一用户码
        String userCode;
        do {
            userCode = RandomUtil.randomNumbers(10);
        } while (userRepository.exists(new LambdaQueryWrapper<User>().eq(User::getUserCode, userCode)));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setUserCode(userCode);
        user.setStatus(Constants.STATUS_OFFLINE);
        userRepository.insert(user);

        log.info("用户注册成功: email={}, userId={}", request.getEmail(), user.getId());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 获取当前 token 版本号（不递增，允许多端同时登录）
        // 版本号仅在密码修改时递增，确保修改密码后所有旧 token 失效
        String versionKey = Constants.REDIS_TOKEN_VERSION + user.getId();
        Object existingVersion = redisUtils.get(versionKey);
        Long tokenVersion;
        if (existingVersion != null) {
            tokenVersion = Long.parseLong(existingVersion.toString());
        } else {
            tokenVersion = 1L;
            redisUtils.set(versionKey, String.valueOf(tokenVersion), 7, TimeUnit.DAYS);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), tokenVersion);

        // 缓存Token到Redis
        redisUtils.set(Constants.REDIS_TOKEN_PREFIX + token,
                user.getId().toString(), 24, TimeUnit.HOURS);

        log.info("用户登录成功: email={}, userId={}, tokenVersion={}", request.getEmail(), user.getId(), tokenVersion);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .userCode(user.getUserCode())
                .build();
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_OLD_PASSWORD_ERROR);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.updateById(user);

        // 递增 token 版本号，使所有旧 token 立即失效
        String versionKey = Constants.REDIS_TOKEN_VERSION + userId;
        redisUtils.increment(versionKey, 1);
        redisUtils.expire(versionKey, 7, TimeUnit.DAYS);

        log.info("密码修改成功，所有旧Token已失效: userId={}", userId);
    }
}