package com.voxel.server.net;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Gan kenh WebSocket the gioi chung vao dia chi /ws/world.
 *
 * setAllowedOrigins("*"): game chay tren may tinh (khong phai trinh duyet) nen khong can
 * chan nguon; cho phep het cho don gian.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WorldSocketHandler handler;
    private final JwtHandshakeInterceptor handshake;

    public WebSocketConfig(WorldSocketHandler handler, JwtHandshakeInterceptor handshake) {
        this.handler = handler;
        this.handshake = handshake;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/world")
                .addInterceptors(handshake)
                .setAllowedOrigins("*");
    }
}
