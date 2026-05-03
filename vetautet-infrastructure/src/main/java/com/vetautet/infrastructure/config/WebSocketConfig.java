package com.vetautet.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client subscribe vào /topic/... để nhận thông báo
        config.enableSimpleBroker("/topic", "/queue");
        // Client gửi message lên server qua prefix /app
        config.setApplicationDestinationPrefixes("/app");
        // Prefix cho gửi tới user cụ thể: /user/{userId}/queue/notifications
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để FE kết nối WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
