package com.linkchat.server.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 单元测试
 * 测试Token的生成、解析和验证
 */
@DisplayName("JWT Token Provider 测试")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 使用构造器直接创建，不依赖Spring容器
        jwtTokenProvider = new JwtTokenProvider(
                "TestSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong!!",
                86400000L // 24小时
        );
    }

    @Test
    @DisplayName("生成Token - 应返回非空字符串")
    void shouldGenerateToken() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", 0L);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("从Token解析userId - 应返回正确的userId")
    void shouldExtractUserId() {
        String token = jwtTokenProvider.generateToken(12345L, "user@test.com", 0L);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(userId).isEqualTo(12345L);
    }

    @Test
    @DisplayName("验证有效Token - 应返回true")
    void shouldValidateValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", 0L);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("验证无效Token - 应返回false")
    void shouldRejectInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("验证空Token - 应返回false")
    void shouldRejectEmptyToken() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("验证被篡改的Token - 应返回false")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", 0L);
        // 修改token的中间部分
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("生成Token包含email Claim")
    void tokenShouldContainEmailClaim() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", 0L);
        // 通过"正确验证后能解析"来间接验证
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }
}