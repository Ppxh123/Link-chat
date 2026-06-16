package com.linkchat.server.security;

import com.linkchat.server.common.BusinessException;
import com.linkchat.server.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * 获取当前用户ID（可能返回 null，调用方需检查）
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 安全获取当前用户ID（绝不返回 null，未认证时抛异常）
     */
    public Long getCurrentUserIdRequired() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 静态方法：安全获取当前用户ID（可能返回 null）
     */
    public static Long getCurrentUserIdSafely() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }
}