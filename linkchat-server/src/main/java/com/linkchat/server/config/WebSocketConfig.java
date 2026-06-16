package com.linkchat.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@EnableWebSocket
public class WebSocketConfig {

    /**
     * 导出 WebSocket 端点，使 @ServerEndpoint 注解生效。
     * WebSocketServer 实例由 JSR-356 容器管理（每次连接创建新实例），
     * 同时 @Component 使 Spring 管理一个单例用于依赖注入。
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}