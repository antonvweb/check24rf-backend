package org.example.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки кодов подтверждения (заглушка)
 */
@Slf4j
@Service
public class EmailService {

    /**
     * Отправка кода верификации (заглушка)
     */
    public void sendVerificationCode(String email, String code) {
        log.info("📧 [MOCK] Отправка кода на email: {}, код: {}", email, code);
    }
}
