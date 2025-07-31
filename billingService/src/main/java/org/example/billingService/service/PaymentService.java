package org.example.billingService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();

    /**
     * Имитация процесса оплаты
     * В реальном приложении здесь будет интеграция с платежным шлюзом
     */
    public boolean processPayment(UUID userId, int amountInCents) {
        logger.info("Обработка платежа для пользователя {}: {} копеек", userId, amountInCents);

        // Имитируем задержку обработки платежа
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Имитируем 95% успешных платежей
        boolean success = random.nextDouble() < 0.95;

        if (success) {
            logger.info("Платеж успешен для пользователя {}", userId);
        } else {
            logger.warn("Ошибка платежа для пользователя {}", userId);
        }

        return success;
    }

    /**
     * Получение URL для редиректа на платежную систему (имитация)
     */
    public String generatePaymentUrl(UUID userId, int amountInCents, String returnUrl) {
        // В реальном приложении здесь будет генерация URL платежной системы
        return String.format("https://mock-payment.example.com/pay?user=%s&amount=%d&return=%s",
                userId, amountInCents, returnUrl);
    }
}
