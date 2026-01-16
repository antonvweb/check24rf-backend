package org.example.mcoService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.dto.response.SendMessageResponse;
import org.example.mcoService.service.McoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/mco")
@RequiredArgsConstructor
public class McoController {

    private final McoService mcoService;
    private final McoApiClient apiClient;

    // ============================================
// ДОБАВЬТЕ ЭТОТ ЭНДПОИНТ В McoController.java
// ============================================

    private final McoProperties mcoProperties; // Добавьте это поле если его нет

    /**
     * ДИАГНОСТИКА - Проверка конфигурации и подключения
     * GET http://localhost:8085/api/mco/diagnose
     */
    @GetMapping("/diagnose")
    public ResponseEntity<String> diagnose() {
        StringBuilder result = new StringBuilder();

        result.append("╔════════════════════════════════════════════════════╗\n");
        result.append("║   🔍 ДИАГНОСТИКА ПОДКЛЮЧЕНИЯ К МЧО                ║\n");
        result.append("╚════════════════════════════════════════════════════╝\n\n");

        // 1. Проверка конфигурации
        result.append("1️⃣ КОНФИГУРАЦИЯ:\n");
        result.append("   • API URL: ").append(mcoProperties.getApi().getBaseUrl()).append("\n");
        result.append("   • Partner INN: ").append(mcoProperties.getPartner().getInn()).append("\n");
        result.append("   • Partner Name: ").append(mcoProperties.getPartner().getName()).append("\n");
        result.append("   • Partner Type: ").append(mcoProperties.getPartner().getType()).append("\n");
        result.append("   • Token: ").append(mcoProperties.getApi().getToken() != null ? "✅ Установлен" : "❌ НЕ установлен").append("\n");
        result.append("   • UserToken: ").append(mcoProperties.getApi().getUserToken() != null ? "✅ Установлен" : "❌ НЕ установлен").append("\n\n");

        // 2. Тест подключения
        result.append("2️⃣ ТЕСТ ПОДКЛЮЧЕНИЯ:\n");
        try {
            // Попробуем отправить простой запрос
            String testRequestId = "DIAG-" + System.currentTimeMillis();
            SendMessageResponse response = apiClient.bindUser("79999999999", testRequestId);

            result.append("   ✅ SOAP запросы работают\n");
            result.append("   • MessageId получен: ").append(response.getMessageId()).append("\n\n");

        } catch (Exception e) {
            result.append("   ❌ ОШИБКА SOAP запроса:\n");
            result.append("   • ").append(e.getMessage()).append("\n\n");
        }

        // 3. Что проверить
        result.append("3️⃣ ЧТО НУЖНО ПРОВЕРИТЬ:\n\n");
        result.append("   ❓ В ЛК МЧО (https://dr.stm-labs.ru/partners):\n");
        result.append("      - Найдите свою учетную запись партнера\n");
        result.append("      - Проверьте что INN совпадает: ").append(mcoProperties.getPartner().getInn()).append("\n");
        result.append("      - Проверьте что есть подключенные пользователи со статусом APPROVED\n");
        result.append("      - Проверьте что у этих пользователей есть чеки\n\n");

        result.append("   ❓ Возможно вы:\n");
        result.append("      - Зарегистрировали ДРУГОГО партнера через API?\n");
        result.append("      - Подключились к ДРУГОМУ партнеру в ЛК?\n");
        result.append("      - Используете старые токены?\n\n");

        // 4. Следующие шаги
        result.append("4️⃣ СЛЕДУЮЩИЕ ШАГИ:\n\n");
        result.append("   Вариант А: Если партнер УЖЕ есть в ЛК:\n");
        result.append("   1. Узнайте PartnerId этого партнера\n");
        result.append("   2. Обновите конфигурацию с правильным PartnerId\n");
        result.append("   3. Повторите тест\n\n");

        result.append("   Вариант Б: Зарегистрировать НОВОГО партнера через API:\n");
        result.append("   1. POST /api/mco/register?logoPath=/path/to/logo.jpg\n");
        result.append("   2. Сохраните полученный PartnerId\n");
        result.append("   3. Подключите пользователя: POST /api/mco/bind-user-test\n");
        result.append("   4. Одобрите заявку в ЛК МЧО\n");
        result.append("   5. Попросите пользователя отсканировать чек\n");
        result.append("   6. Повторите тест через 2-3 минуты\n\n");

        result.append("╔════════════════════════════════════════════════════╗\n");
        result.append("║   💡 СОВЕТ                                         ║\n");
        result.append("╚════════════════════════════════════════════════════╝\n\n");
        result.append("Скорее всего проблема в том что:\n");
        result.append("• Ваши токены и конфигурация для одного партнера\n");
        result.append("• А в ЛК вы подключились к ДРУГОМУ партнеру\n");
        result.append("• API не видит чеки потому что ищет у СВОЕГО партнера\n\n");

        return ResponseEntity.ok(result.toString());
    }

    // ==========================================
    // РЕГИСТРАЦИЯ И ПОДКЛЮЧЕНИЕ
    // ==========================================

    /**
     * Регистрация партнера в системе МЧО
     * POST http://localhost:8085/api/mco/register?logoPath=/path/to/logo.jpg
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerPartner(@RequestParam String logoPath) {
        try {
            String partnerId = mcoService.initializePartner(logoPath);
            return ResponseEntity.ok(
                    "✅ Партнер успешно зарегистрирован!\n" +
                            "ID партнера: " + partnerId + "\n\n" +
                            "Проверьте в ЛК МЧО: https://dr.stm-labs.ru/partners"
            );
        } catch (Exception e) {
            log.error("Ошибка регистрации партнера", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка регистрации: " + e.getMessage());
        }
    }

    /**
     * Подключение пользователя (с указанием номера)
     * POST http://localhost:8085/api/mco/bind-user?phone=79999999999
     */
    @PostMapping("/bind-user")
    public ResponseEntity<String> bindUser(@RequestParam String phone) {
        try {
            String messageId = mcoService.connectUser(phone);
            return ResponseEntity.ok(
                    "✅ Заявка на подключение отправлена!\n" +
                            "MessageId: " + messageId + "\n\n" +
                            "⚠️ ВАЖНО: Пользователь должен одобрить заявку в ЛК МЧО:\n" +
                            "https://dr.stm-labs.ru/"
            );
        } catch (Exception e) {
            log.error("Ошибка подключения пользователя", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка подключения: " + e.getMessage());
        }
    }

    /**
     * Подключение тестового пользователя (фиксированный номер)
     * POST http://localhost:8085/api/mco/bind-user-test
     */
    @PostMapping("/bind-user-test")
    public ResponseEntity<String> bindUserTest() {
        String testPhone = "79054455906";

        try {
            String messageId = mcoService.connectUser(testPhone);
            return ResponseEntity.ok(
                    "✅ Тестовая заявка на подключение отправлена!\n" +
                            "Телефон: " + testPhone + "\n" +
                            "MessageId: " + messageId + "\n\n" +
                            "СЛЕДУЮЩИЕ ШАГИ:\n" +
                            "1. Зайдите в ЛК МЧО: https://dr.stm-labs.ru/partners\n" +
                            "2. Найдите заявку от пользователя " + testPhone + "\n" +
                            "3. Одобрите заявку\n" +
                            "4. Попросите пользователя отсканировать чек в приложении МЧО\n" +
                            "5. Проверьте получение чеков через /test-receipts"
            );
        } catch (Exception e) {
            log.error("Ошибка подключения тестового пользователя", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }

    // ==========================================
    // РАБОТА С ЧЕКАМИ
    // ==========================================

    /**
     * ТЕСТОВЫЙ ЭНДПОИНТ - Получение одной порции чеков для демонстрации
     * GET http://localhost:8085/api/mco/test-receipts
     */
    @GetMapping("/test-receipts")
    public ResponseEntity<String> testReceipts() {
        try {
            log.info(">>> ЗАПУСК ТЕСТОВОГО ПОЛУЧЕНИЯ ЧЕКОВ <<<");

            int receiptsCount = mcoService.testReceiptsOnce();

            if (receiptsCount == 0) {
                return ResponseEntity.ok(
                        "⚠️ Чеков не найдено!\n\n" +
                                "ВОЗМОЖНЫЕ ПРИЧИНЫ:\n" +
                                "1. Нет подключенных пользователей\n" +
                                "2. Пользователи не сканировали чеки\n" +
                                "3. Прошло больше 5 дней с момента сканирования\n\n" +
                                "ЧТО ДЕЛАТЬ:\n" +
                                "1. Подключите пользователя: POST /bind-user-test\n" +
                                "2. Одобрите заявку в ЛК: https://dr.stm-labs.ru/partners\n" +
                                "3. Отсканируйте чек в приложении МЧО\n" +
                                "4. Подождите 2-3 минуты и повторите запрос\n\n" +
                                "📋 Смотрите подробные логи в консоли приложения!"
                );
            }

            return ResponseEntity.ok(
                    "✅ УСПЕШНО!\n\n" +
                            "Получено чеков: " + receiptsCount + "\n\n" +
                            "📋 Смотрите подробную информацию в логах!\n" +
                            "💡 Для полной синхронизации используйте GET /sync-receipts"
            );

        } catch (Exception e) {
            log.error("Ошибка тестирования получения чеков", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage() + "\n\n" +
                            "📋 Смотрите подробности в логах!");
        }
    }

    /**
     * ДЕТАЛЬНЫЙ ТЕСТ - Получение чеков с полным выводом всей информации
     * GET http://localhost:8085/api/mco/test-receipts-detailed
     */
    @GetMapping("/test-receipts-detailed")
    public ResponseEntity<String> testReceiptsDetailed() {
        try {
            mcoService.detailedReceiptsTest();
            return ResponseEntity.ok(
                    "✅ Детальный тест завершен!\n\n" +
                            "📋 Вся информация выведена в логах.\n" +
                            "Смотрите консоль приложения для полных деталей."
            );
        } catch (Exception e) {
            log.error("Ошибка детального теста", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * ПОЛНАЯ СИНХРОНИЗАЦИЯ - Получение всех доступных чеков с пагинацией
     * GET http://localhost:8085/api/mco/sync-receipts
     */
    @GetMapping("/sync-receipts")
    public ResponseEntity<String> syncReceipts() {
        try {
            log.info(">>> ЗАПУСК ПОЛНОЙ СИНХРОНИЗАЦИИ ЧЕКОВ <<<");

            mcoService.syncReceipts();

            return ResponseEntity.ok(
                    "✅ Полная синхронизация завершена!\n\n" +
                            "📋 Все чеки обработаны.\n" +
                            "Смотрите подробности в логах приложения."
            );

        } catch (Exception e) {
            log.error("Ошибка синхронизации чеков", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка синхронизации: " + e.getMessage());
        }
    }

    /**
     * СТАТИСТИКА - Получение информации о доступных чеках
     * GET http://localhost:8085/api/mco/receipts-stats
     */
    @GetMapping("/receipts-stats")
    public ResponseEntity<String> getReceiptsStats() {
        try {
            String stats = mcoService.getReceiptsStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Ошибка получения статистики", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * ПОЛУЧЕНИЕ ПО МАРКЕРУ - Получение конкретной порции чеков
     * GET http://localhost:8085/api/mco/receipts?marker=S_FROM_END
     */
    @GetMapping("/receipts")
    public ResponseEntity<?> getReceiptsByMarker(
            @RequestParam(defaultValue = "S_FROM_END") String marker) {
        try {
            GetReceiptsTapeResponse response = mcoService.getReceiptsByMarker(marker);

            // Формируем читаемый ответ
            StringBuilder result = new StringBuilder();
            result.append("✅ Получена порция чеков\n\n");

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                result.append("Количество чеков: ").append(response.getReceipts().size()).append("\n\n");

                result.append("Чеки:\n");
                response.getReceipts().forEach(receipt -> {
                    result.append("  - Пользователь: ").append(receipt.getUserIdentifier())
                            .append(", Дата: ").append(receipt.getReceiveDate())
                            .append(", Источник: ").append(receipt.getSourceCode())
                            .append("\n");
                });
            } else {
                result.append("Чеков в этой порции нет.\n");
            }

            result.append("\nNextMarker: ").append(response.getNextMarker()).append("\n");
            result.append("Осталось порций: ").append(response.getTotalExpectedRemainingPolls()).append("\n");

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            log.error("Ошибка получения чеков по маркеру", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }

    // ==========================================
    // СЛУЖЕБНЫЕ ЭНДПОИНТЫ
    // ==========================================

    /**
     * Проверка работоспособности сервиса
     * GET http://localhost:8085/api/mco/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(
                "✅ МЧО Сервис работает!\n\n" +
                        "Доступные эндпоинты:\n" +
                        "📝 Регистрация и подключение:\n" +
                        "  POST /api/mco/register?logoPath=... - Регистрация партнера\n" +
                        "  POST /api/mco/bind-user?phone=... - Подключение пользователя\n" +
                        "  POST /api/mco/bind-user-test - Подключение тестового пользователя\n\n" +
                        "📋 Работа с чеками:\n" +
                        "  GET /api/mco/test-receipts - Тестовое получение чеков\n" +
                        "  GET /api/mco/test-receipts-detailed - Детальный тест\n" +
                        "  GET /api/mco/sync-receipts - Полная синхронизация\n" +
                        "  GET /api/mco/receipts-stats - Статистика по чекам\n" +
                        "  GET /api/mco/receipts?marker=... - Получение по маркеру\n\n" +
                        "🔧 Служебные:\n" +
                        "  GET /api/mco/health - Проверка работоспособности"
        );
    }

    // ============================================
// ДОБАВЬТЕ ЭТИ ЭНДПОИНТЫ В McoController.java
// ============================================

    /**
     * ТЕСТ С МАРКЕРОМ S_FROM_BEGINNING
     * GET http://localhost:8085/api/mco/test-receipts-from-beginning
     */
    @GetMapping("/test-receipts-from-beginning")
    public ResponseEntity<String> testReceiptsFromBeginning() {
        try {
            log.info(">>> ТЕСТ С МАРКЕРОМ S_FROM_BEGINNING <<<");

            GetReceiptsTapeResponse response = apiClient.getReceiptsSync("S_FROM_BEGINNING");

            StringBuilder result = new StringBuilder();
            result.append("✅ Запрос выполнен успешно!\n\n");

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                result.append("📋 Получено чеков: ").append(response.getReceipts().size()).append("\n\n");

                response.getReceipts().forEach(receipt -> {
                    result.append("  - Пользователь: ").append(receipt.getUserIdentifier())
                            .append(", Дата: ").append(receipt.getReceiveDate())
                            .append(", Источник: ").append(receipt.getSourceCode())
                            .append("\n");
                });

                result.append("\nNextMarker: ").append(response.getNextMarker()).append("\n");
                result.append("Осталось порций: ").append(response.getTotalExpectedRemainingPolls()).append("\n");
            } else {
                result.append("❌ Чеков не найдено\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            log.error("Ошибка теста с S_FROM_BEGINNING", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * ТЕСТ БЕЗ МАРКЕРА (пустая строка)
     * GET http://localhost:8085/api/mco/test-receipts-no-marker
     */
    @GetMapping("/test-receipts-no-marker")
    public ResponseEntity<String> testReceiptsNoMarker() {
        try {
            log.info(">>> ТЕСТ БЕЗ МАРКЕРА (пустая строка) <<<");

            GetReceiptsTapeResponse response = apiClient.getReceiptsSync("");

            StringBuilder result = new StringBuilder();
            result.append("✅ Запрос выполнен успешно!\n\n");

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                result.append("📋 Получено чеков: ").append(response.getReceipts().size()).append("\n\n");

                response.getReceipts().forEach(receipt -> {
                    result.append("  - Пользователь: ").append(receipt.getUserIdentifier())
                            .append(", Дата: ").append(receipt.getReceiveDate())
                            .append(", Источник: ").append(receipt.getSourceCode())
                            .append("\n");
                });

                result.append("\nNextMarker: ").append(response.getNextMarker()).append("\n");
                result.append("Осталось порций: ").append(response.getTotalExpectedRemainingPolls()).append("\n");
            } else {
                result.append("❌ Чеков не найдено\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            log.error("Ошибка теста без маркера", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }

    /**
     * ТЕСТ С NULL МАРКЕРОМ
     * GET http://localhost:8085/api/mco/test-receipts-null-marker
     */
    @GetMapping("/test-receipts-null-marker")
    public ResponseEntity<String> testReceiptsNullMarker() {
        try {
            log.info(">>> ТЕСТ С NULL МАРКЕРОМ <<<");

            GetReceiptsTapeResponse response = apiClient.getReceiptsSync(null);

            StringBuilder result = new StringBuilder();
            result.append("✅ Запрос выполнен успешно!\n\n");

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                result.append("📋 Получено чеков: ").append(response.getReceipts().size()).append("\n\n");

                response.getReceipts().forEach(receipt -> {
                    result.append("  - Пользователь: ").append(receipt.getUserIdentifier())
                            .append(", Дата: ").append(receipt.getReceiveDate())
                            .append(", Источник: ").append(receipt.getSourceCode())
                            .append("\n");
                });

                result.append("\nNextMarker: ").append(response.getNextMarker()).append("\n");
                result.append("Осталось порций: ").append(response.getTotalExpectedRemainingPolls()).append("\n");
            } else {
                result.append("❌ Чеков не найдено\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            log.error("Ошибка теста с null маркером", e);
            return ResponseEntity.status(500)
                    .body("❌ Ошибка: " + e.getMessage());
        }
    }
}