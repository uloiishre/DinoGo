package com.dinogo.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.dinogo.chat.service.DinoChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class DinoChatWebSocketConfig implements WebSocketConfigurer {

    private final DinoChatWebSocketHandler handler;

    public DinoChatWebSocketConfig(DinoChatWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/dino-chat")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:8088", "https://dinogo-shop.site");
    }
}
