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
        
        // Для email используем default chat_id
        if (defaultChatId != null && !defaultChatId.isBlank()) {
            try {
                Long chatId = Long.parseLong(defaultChatId);
                telegramBotService.sendVerificationCode(chatId, code);
                log.info("✉️ Код отправлен в Telegram (email: {}, chat_id: {})", email, defaultChatId);
                return;
            } catch (NumberFormatException e) {
                log.warn("Некорректный Telegram chat_id: {}", defaultChatId);
            }
        }
        
        log.warn("Telegram chat_id не настроен, код не будет отправлен");
    }
}
