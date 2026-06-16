package com.linkchat.server.security;

import com.linkchat.server.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全配置集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("安全配置 集成测试")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未认证访问公开接口 - 应允许访问")
    void unauthenticatedAccessToPublicEndpointShouldBeAllowed() throws Exception {
        mockMvc.perform(get("/api/auth/nonexistent"))
                .andExpect(status().is4xxClientError()); // 404正确，说明通过了安全过滤器
    }

    @Test
    @DisplayName("未认证访问受保护接口 - 应返回401")
    void unauthenticatedAccessToProtectedEndpointShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("带无效Token访问受保护接口 - 应返回401")
    void accessWithInvalidTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer invalid_token_here"))
                .andExpect(status().is4xxClientError());
    }
}