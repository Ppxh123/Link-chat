package com.linkchat.server.config;

import com.linkchat.server.security.SecurityUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置
 * 基于令牌桶算法的简易限流，防止接口被刷
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    private final SecurityUtils securityUtils;

    /**
     * 简单限流过滤器
     * 每用户每秒最多100次请求
     */
    @Bean
    public FilterRegistrationBean<Filter> rateLimitFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(securityUtils));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    static class RateLimitFilter implements Filter {

        private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final SecurityUtils securityUtils;

        RateLimitFilter(SecurityUtils securityUtils) {
            this.securityUtils = securityUtils;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            String key = buildKey(httpReq);

            TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(100, 100));
            if (!bucket.tryConsume()) {
                HttpServletResponse httpResp = (HttpServletResponse) response;
                httpResp.setStatus(429);
                httpResp.setContentType("application/json;charset=UTF-8");
                httpResp.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
            chain.doFilter(request, response);
        }

        private String buildKey(HttpServletRequest request) {
            Long userId = securityUtils.getCurrentUserId();
            if (userId != null) {
                return "user:" + userId;
            }
            return "ip:" + request.getRemoteAddr();
        }
    }

    /**
     * 令牌桶实现
     */
    static class TokenBucket {
        private final int capacity;
        private final double refillRate; // 每秒补充的令牌数
        private double tokens;
        private long lastRefillTime;

        TokenBucket(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTime) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRate);
            lastRefillTime = now;
        }
    }
}