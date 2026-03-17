package com.byteferry.byteferry.config;

import com.byteferry.byteferry.websocket.FriendWebSocketHandler;
import com.byteferry.byteferry.websocket.SpaceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SpaceWebSocketHandler spaceWebSocketHandler;
    private final FriendWebSocketHandler friendWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(spaceWebSocketHandler, "/ws/space")
                .setAllowedOrigins("*");
        registry.addHandler(friendWebSocketHandler, "/ws/friend")
                .setAllowedOrigins("*");
    }
}
