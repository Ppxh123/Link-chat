package com.linkchat.server.security;

import com.linkchat.server.common.Constants;
import com.linkchat.server.util.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                // 验证 token 版本号（密码修改后旧 token 失效，允许同一版本多端登录）
                Long tokenVersion = jwtTokenProvider.getTokenVersionFromToken(token);
                String versionKey = Constants.REDIS_TOKEN_VERSION + userId;
                Object currentVersion = redisUtils.get(versionKey);
                if (currentVersion != null) {
                    long current = Long.parseLong(currentVersion.toString());
                    if (tokenVersion < current) {
                        log.warn("Token版本号过期（密码已修改）: userId={}, tokenVersion={}, currentVersion={}",
                                userId, tokenVersion, current);
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT认证成功: userId={}", userId);
            } catch (Exception e) {
                log.error("JWT认证处理异常: {}", e.getMessage());
                // 认证失败时继续过滤链，不阻塞请求
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") || path.startsWith("/ws");
    }
}