package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class McoService {

    private final McoApiClient apiClient;
    private final McoProperties properties;

    /**
     * Регистрация партнера в системе МЧО
     * @param logoPath путь к логотипу компании (JPEG, до 100 КБ)
     * @return ID зарегистрированного партнера
     */
    public String initializePartner(String logoPath) {
        try {
            byte[] logoBytes = Files.readAllBytes(Path.of(logoPath));
            if (logoBytes.length > 100 * 1024) {
                throw new IllegalArgumentException("Размер логотипа превышает 100 КБ");
            }

            String mimeType = Files.probeContentType(Path.of(logoPath));
            if (!"image/jpeg".equals(mimeType)) {
                throw new IllegalArgumentException("Логотип должен быть в формате JPEG");
            }

            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);

            PostPlatformRegistrationResponse response = apiClient.registerPartnerSync(
                    properties.getPartner().getName(),
                    "Платформа для начисления бонусов и кешбэка",
                    "https://xn--24-mlcu7d.xn--p1ai/",
                    base64Logo,
                    properties.getPartner().getInn(),
                    "79991234567"
            );

            log.info("Партнер успешно зарегистрирован, ID: {}", response.getId());
            return response.getId();

        } catch (IOException e) {
            log.error("Ошибка чтения логотипа", e);
            throw new RuntimeException("Не удалось загрузить логотип", e);
        }
    }

    /**
     * Подключение пользователя к партнеру
     * @param phone номер телефона пользователя
     * @return MessageId для отслеживания статуса заявки
     */
    public String connectUser(String phone) {
        String requestId = UUID.randomUUID().toString().toUpperCase();

        PostBindPartnerResponse response = apiClient.bindUserSync(phone, requestId);

        log.info("Заявка на подключение пользователя {} отправлена, MessageId: {}, RequestId: {}",
                phone, response.getMessageId(), requestId);

        return response.getMessageId();
    }

    /**
     * Полная синхронизация чеков (получение всех доступных чеков с пагинацией)
     */
    public void syncReceipts() {
        log.info("=== НАЧАЛО ПОЛНОЙ СИНХРОНИЗАЦИИ ЧЕКОВ ===");
        apiClient.getAllReceiptsSync();
        log.info("=== СИНХРОНИЗАЦИЯ ЗАВЕРШЕНА ===");
    }

    /**
     * Тестовое получение одной порции чеков
     * @return количество полученных чеков
     */
    public int testReceiptsOnce() {
        log.info(">>> ТЕСТОВОЕ ПОЛУЧЕНИЕ ЧЕКОВ <<<");

        try {
            GetReceiptsTapeResponse response = apiClient.getReceiptsSync("S_FROM_END");

            if (response.getReceipts() == null || response.getReceipts().isEmpty()) {
                log.warn("⚠️ Чеков не найдено!");
                log.info("Возможные причины:");
                log.info("  1. Нет подключенных пользователей");
                log.info("  2. Пользователи не сканировали чеки");
                log.info("  3. Прошло больше 5 дней с момента сканирования");
                return 0;
            }

            log.info("✅ Успешно получено {} чеков", response.getReceipts().size());

            // Выводим информацию о первом чеке для демонстрации
            var firstReceipt = response.getReceipts().get(0);
            log.info("Пример чека:");
            log.info("  - Пользователь: {}", firstReceipt.getUserIdentifier());
            log.info("  - Дата: {}", firstReceipt.getReceiveDate());
            log.info("  - Источник: {}", firstReceipt.getSourceCode());

            return response.getReceipts().size();

        } catch (Exception e) {
            log.error("❌ Ошибка при тестировании получения чеков", e);
            throw new RuntimeException("Ошибка получения чеков: " + e.getMessage(), e);
        }
    }

    /**
     * Детальный тест получения чеков с полным выводом информации
     */
    public void detailedReceiptsTest() {
        log.info("=== ДЕТАЛЬНЫЙ ТЕСТ ПОЛУЧЕНИЯ ЧЕКОВ ===");
        apiClient.testReceiptsFlow();
    }

    /**
     * Получение одной порции чеков по маркеру
     * @param marker маркер для получения чеков (S_FROM_END, S_FROM_BEGINNING, или полученный NextMarker)
     * @return ответ с чеками и следующим маркером
     */
    public GetReceiptsTapeResponse getReceiptsByMarker(String marker) {
        log.info("Получение чеков по маркеру: {}", marker);
        return apiClient.getReceiptsSync(marker);
    }

    /**
     * Получение статистики по чекам (сколько всего доступно)
     * @return строка со статистикой
     */
    public String getReceiptsStats() {
        try {
            GetReceiptsTapeResponse response = apiClient.getReceiptsSync("S_FROM_END");

            int receiptsCount = response.getReceipts() != null ? response.getReceipts().size() : 0;
            Long remainingPolls = response.getTotalExpectedRemainingPolls();

            return String.format(
                    "Статистика чеков:\n" +
                            "  - Получено в текущей порции: %d\n" +
                            "  - Осталось порций для загрузки: %d\n" +
                            "  - NextMarker: %s",
                    receiptsCount,
                    remainingPolls != null ? remainingPolls : 0,
                    response.getNextMarker()
            );

        } catch (Exception e) {
            log.error("Ошибка получения статистики", e);
            return "Ошибка: " + e.getMessage();
        }
    }
}