package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class McoService {

    private final McoApiClient apiClient;
    private final McoProperties properties;
    private final McoApiClient mcoApiClient;
    private final ReceiptService receiptService;

    public GetUnboundPartnerResponse getUnboundPartners(String marker) {
        return mcoApiClient.getUnboundPartners(marker);
    }

    public PostNotificationResponse sendNotification(
            String requestId,
            String phoneNumber,
            String title,
            String message,
            String shortMessage,
            String category,
            String externalItemId,
            String externalItemUrl) {
        return mcoApiClient.sendNotification(
                requestId, phoneNumber, title, message,
                shortMessage, category, externalItemId, externalItemUrl
        );
    }

    /**
     * Получение статуса заявки на подключение пользователя
     *
     * @param requestId идентификатор заявки
     * @return информация о статусе заявки
     */
    public GetBindPartnerStatusResponse.BindPartnerStatus checkBindRequestStatus(String requestId) {
        log.info("Проверка статуса заявки: {}", requestId);

        GetBindPartnerStatusResponse response = apiClient.getBindRequestStatusSync(
                Collections.singletonList(requestId)
        );

        if (response.getStatuses() == null || response.getStatuses().isEmpty()) {
            throw new RuntimeException("Не получен статус для requestId: " + requestId);
        }

        return response.getStatuses().get(0);
    }

    /**
     * Получение статусов нескольких заявок
     *
     * @param requestIds список идентификаторов заявок (до 50 штук)
     * @return список статусов
     */
    public List<GetBindPartnerStatusResponse.BindPartnerStatus> checkBindRequestStatuses(List<String> requestIds) {
        log.info("Проверка статусов заявок, количество: {}", requestIds.size());

        if (requestIds.size() > 50) {
            throw new IllegalArgumentException("Максимум 50 requestIds за один запрос");
        }

        GetBindPartnerStatusResponse response = apiClient.getBindRequestStatusSync(requestIds);

        return response.getStatuses() != null ? response.getStatuses() : Collections.emptyList();
    }

    /**
     * Проверка статуса последней заявки для пользователя
     * Для удобства тестирования
     *
     * @param phone номер телефона пользователя
     * @return статус заявки (если она была создана через connectUser)
     */
    public String checkUserBindStatus(String phone) {
        // Примечание: для полноценной работы нужно сохранять requestId при вызове connectUser
        // Пока что это демонстрационный метод
        log.warn("Метод checkUserBindStatus требует сохранения requestId при создании заявки");
        throw new UnsupportedOperationException("Нужно сохранять requestId в базу при вызове connectUser");
    }

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

    // ============================================
// УЛУЧШЕННАЯ ВЕРСИЯ connectUser
// ЗАМЕНИТЬ существующий метод connectUser в McoService
// ============================================

    /**
     * Подключение пользователя к партнеру
     * @param phone номер телефона пользователя
     * @return RequestId для отслеживания статуса заявки (НЕ MessageId!)
     */
    public String connectUser(String phone) {
        // Генерируем RequestId - именно его нужно сохранять для проверки статуса!
        String requestId = UUID.randomUUID().toString().toUpperCase();

        log.info("Создание заявки на подключение пользователя {}, RequestId: {}", phone, requestId);

        PostBindPartnerResponse response = apiClient.bindUserSync(phone, requestId);

        log.info("✅ Заявка на подключение отправлена");
        log.info("   RequestId: {} - СОХРАНИТЕ ЕГО для проверки статуса!", requestId);
        log.info("   MessageId: {}", response.getMessageId());
        log.info("   Проверить статус: GET /api/mco/bind-request-status?requestId={}", requestId);

        // ВАЖНО: возвращаем RequestId, а не MessageId!
        // RequestId нужен для метода GetBindPartnerStatusRequest
        return requestId;
    }

    public PostBindPartnerBatchResponse bindUsersBatch(String requestId, List<String> phoneNumbers) {
        return mcoApiClient.bindUsersBatch(requestId, phoneNumbers);
    }

    public GetBindPartnerEventResponse getBindPartnerEvents(String marker) {
        return mcoApiClient.getBindPartnerEvents(marker);
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
        GetReceiptsTapeResponse response = apiClient.getReceiptsSync(marker);

        // ✅ АВТОМАТИЧЕСКИ СОХРАНЯЕМ ЧЕКИ В БД
        if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
            int savedCount = receiptService.saveReceipts(response.getReceipts());
            log.info("Автоматически сохранено {} новых чеков в БД", savedCount);
        }

        return response;
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