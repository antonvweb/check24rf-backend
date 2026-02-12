package org.example.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки email
 */
@Slf4j
@Service
public class EmailService {
    
    public void sendVerificationCode(String email, String code) {
        log.info("📧 [MOCK] Отправка кода на email: {}, код: {}", email, code);
    }
}
