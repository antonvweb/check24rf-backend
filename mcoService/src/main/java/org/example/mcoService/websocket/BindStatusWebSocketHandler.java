package org.example.mcoService.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Единый WebSocket обработчик для всех уведомлений МЧО:
 * - Статус подключения (bind/unbind)
 * - Новые чеки
 * - Отключение пользователей
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BindStatusWebSocketHandler extends TextWebSocketHandler {

    // Хранение сессий по requestId
    private final Map<String, WebSocketSession> sessionsByRequestId = new ConcurrentHashMap<>();
    
    // Хранение сессий по phoneNumber (для возможности отправки по номеру телефона)
    private final Map<String, WebSocketSession> sessionsByPhone = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket подключение установлено: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("Получено сообщение от {}: {}", session.getId(), payload);

        // Ожидаем сообщение формата: {"type": "SUBSCRIBE", "requestId": "...", "phone": "..."}
        try {
            var messageData = parseMessage(payload);
            
            if ("SUBSCRIBE".equals(messageData.get("type"))) {
                String requestId = messageData.get("requestId");
                String phone = messageData.get("phone");
                
                if (requestId != null) {
                    sessionsByRequestId.put(requestId, session);
                    log.info("Подписка на requestId: {}", requestId);
                }
                
                if (phone != null) {
                    sessionsByPhone.put(phone, session);
                    log.info("Подписка на phone: {}", phone);
                }
                
                // Отправляем подтверждение подписки
                sendMessage(session, "{\"type\": \"SUBSCRIBED\", \"status\": \"success\"}");
            }
            
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения: {}", e.getMessage());
            sendMessage(session, "{\"type\": \"ERROR\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket подключение закрыто: {} (status: {})", session.getId(), status);
        
        // Удаляем сессии из мапов
        sessionsByRequestId.values().removeIf(s -> s.getId().equals(session.getId()));
        sessionsByPhone.values().removeIf(s -> s.getId().equals(session.getId()));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Ошибка транспорта WebSocket {}: {}", session.getId(), exception.getMessage());
    }

    /**
     * Отправить уведомление о статусе подключения по requestId
     */
    public void sendBindStatusNotification(String requestId, String status, String phone) {
        WebSocketSession session = sessionsByRequestId.get(requestId);
        
        if (session != null && session.isOpen()) {
            String message = String.format(
                    "{\"type\": \"BIND_STATUS\", \"requestId\": \"%s\", \"status\": \"%s\", \"phone\": \"%s\"}",
                    requestId, status, phone
            );
            sendMessage(session, message);
            log.info("Отправлено уведомление о статусе для requestId: {}, status: {}", requestId, status);
        } else {
            log.warn("Нет активной WebSocket сессии для requestId: {}", requestId);
        }
    }

    /**
     * Отправить уведомление о новых чеках по phoneNumber
     */
    public void sendNewReceiptsNotification(String phone, int count, String totalAmount) {
        WebSocketSession session = sessionsByPhone.get(phone);
        
        if (session != null && session.isOpen()) {
            String message = String.format(
                    "{\"type\": \"NEW_RECEIPTS\", \"phone\": \"%s\", \"count\": %d, \"totalAmount\": \"%s\"}",
                    phone, count, totalAmount
            );
            sendMessage(session, message);
            log.info("Отправлено уведомление о новых чеках для {}: {} чеков", phone, count);
        } else {
            log.debug("Нет активной WebSocket сессии для phone: {}", phone);
        }
    }

    /**
     * Отправить уведомление об отключении пользователя
     */
    public void sendUnbindNotification(String phone, String reason) {
        WebSocketSession session = sessionsByPhone.get(phone);

        if (session != null && session.isOpen()) {
            String message = String.format(
                    "{\"type\": \"UNBIND\", \"phone\": \"%s\", \"reason\": \"%s\", \"timestamp\": \"%s\"}",
                    phone,
                    reason != null ? reason : "Пользователь отключился",
                    java.time.LocalDateTime.now().toString()
            );
            sendMessage(session, message);
            log.info("Отправлено уведомление об отключении для {}", phone);
        } else {
            log.debug("Нет активной WebSocket сессии для phone: {}", phone);
        }
    }

    /**
     * Отправить уведомление об ошибке
     */
    public void sendErrorNotification(String requestId, String error) {
        WebSocketSession session = sessionsByRequestId.get(requestId);
        
        if (session != null && session.isOpen()) {
            String message = String.format(
                    "{\"type\": \"ERROR\", \"requestId\": \"%s\", \"message\": \"%s\"}",
                    requestId, error
            );
            sendMessage(session, message);
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private Map<String, String> parseMessage(String payload) {
        // Простой парсинг JSON без дополнительных зависимостей
        Map<String, String> result = new ConcurrentHashMap<>();
        
        payload = payload.trim();
        if (payload.startsWith("{") && payload.endsWith("}")) {
            payload = payload.substring(1, payload.length() - 1);
            String[] pairs = payload.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
}
