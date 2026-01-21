package org.example.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки email
 * TODO: Реализовать интеграцию с email провайдером
 */
@Slf4j
@Service
public class EmailService {
    
    public void sendVerificationCode(String email, String code) {
        // TODO: Интеграция с email провайдером (SendGrid, AWS SES, и т.д.)
        log.info("📧 [MOCK] Отправка кода на email: {}, код: {}", email, code);
        
        // Временная заглушка - код просто логируется
        // В продакшене здесь должна быть реальная отправка email
    }
}
