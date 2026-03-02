package org.example.mcoService.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.websocket.BindStatusWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final BindStatusWebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Путь должен совпадать с тем, что приходит от nginx
        // nginx проксирует /api/mco/ → http://127.0.0.1:17456
        // Значит запрос /api/mco/ws придёт на mcoService как /api/mco/ws
        registry.addHandler(webSocketHandler, "/api/mco/ws")
                .setAllowedOrigins(
                    "https://xn--24-mlcu7d.xn--p1ai",
                    "https://www.xn--24-mlcu7d.xn--p1ai"
                );

        // Дополнительный endpoint для локальной разработки (без nginx)
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("*");

        log.info("WebSocket handlers registered on /api/mco/ws and /ws");
    }
}
