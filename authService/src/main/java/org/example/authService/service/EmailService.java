package org.example.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки кодов подтверждения через Telegram
 */
@Slf4j
@Service
public class EmailService {

    private final TelegramBotService telegramBotService;

    @Value("${telegram.chat.id:}")
    private String defaultChatId;

    public EmailService(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    /**
     * Отправка кода верификации через Telegram
     */
    public void sendVerificationCode(String email, String code) {
        log.info("📧 Отправка кода на email через Telegram: {}, код: {}", email, code);
        
        // Используем default chat id
        if (defaultChatId != null && !defaultChatId.isBlank()) {
            try {
                Long chatId = Long.parseLong(defaultChatId);
                telegramBotService.sendVerificationCode(chatId, code);
                return;
            } catch (NumberFormatException e) {
                log.warn("Некорректный Telegram chat_id: {}", defaultChatId);
            }
        }
        
        // Если default chat_id не настроен, отправляем всем
        telegramBotService.broadcastMessage("🔐 Ваш код подтверждения: *" + code + "*");
    }
}
