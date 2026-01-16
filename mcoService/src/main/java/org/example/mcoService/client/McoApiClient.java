package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.request.GetReceiptsTapeRequest;
import org.example.mcoService.dto.request.PostBindPartnerRequest;
import org.example.mcoService.dto.request.PostPlatformRegistrationRequest;
import org.example.mcoService.dto.request.SendMessageRequest;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.example.mcoService.dto.response.SendMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class McoApiClient {

    @Autowired
    private McoSoapClient soapClient;

    public SendMessageResponse registerPartner(
            String name,
            String description,
            String transitionLink,
            String base64Logo,
            String inn,
            String phone) {

        log.info("=== НАЧАЛО РЕГИСТРАЦИИ ===");
        log.info("name: [{}]", name);
        log.info("name length: {}", name != null ? name.length() : "null");
        log.info("name bytes: {}", name != null ? java.util.Arrays.toString(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)) : "null");
        log.info("type: PARTNER");
        log.info("inn: {}", inn);
        log.info("phone: {}", phone);

        PostPlatformRegistrationRequest innerRequest = PostPlatformRegistrationRequest.builder()
                .name(name)
                .type("PARTNER")
                .description(description)
                .transitionLink(transitionLink)
                .text(description)
                .image(base64Logo != null ? base64Logo : "")
                .imageFullScreen("")
                .inn(inn)
                .phone(phone)
                .build();

        log.info("=== СОЗДАН ОБЪЕКТ ===");
        log.info("innerRequest.name: [{}]", innerRequest.getName());

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse response = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Партнер зарегистрирован, MessageId: {}", response.getMessageId());
        return response;
    }

    public PostPlatformRegistrationResponse registerPartnerSync(
            String name,
            String description,
            String transitionLink,
            String base64Logo,
            String inn,
            String phone) {

        log.info("Регистрация партнера: {}", name);

        // Отправляем запрос
        SendMessageResponse messageResponse = registerPartner(
                name, description, transitionLink, base64Logo, inn, phone
        );

        log.info("Получен MessageId: {}, ожидаем результата...", messageResponse.getMessageId());

        try {
            // Опрашиваем результат
            PostPlatformRegistrationResponse response =
                    soapClient.getAsyncResult(
                            messageResponse.getMessageId(),
                            PostPlatformRegistrationResponse.class
                    );

            log.info("Партнер зарегистрирован, ID: {}", response.getId());
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }

    public SendMessageResponse bindUser(String phoneNumber, String requestId) {
        log.info("Подключение пользователя: {}", phoneNumber);

        PostBindPartnerRequest innerRequest = PostBindPartnerRequest.builder()
                .requestId(requestId)
                .userIdentifier(phoneNumber)
                .permissionGroups(Collections.singletonList("DEFAULT"))
                .expiredAt(LocalDateTime.now().plusDays(7))
                .isUnverifiedIdentifier(false)
                .requireNoActiveRequests(false)
                .build();

        // ←←← ВОТ СЮДА ТОЖЕ!
        log.info(">>> ОТПРАВЛЯЕМ PostBindPartnerRequest: requestId = {}, userIdentifier = {}",
                innerRequest.getRequestId(), innerRequest.getUserIdentifier());

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        return soapClient.sendSoapRequest(request, SendMessageResponse.class, "SendMessageRequest");
    }

    public GetReceiptsTapeResponse getReceipts(String marker) {
        log.info("Получение ленты чеков с маркером: {}", marker);

        GetReceiptsTapeRequest request = GetReceiptsTapeRequest.builder()
                .marker(marker != null ? marker : "S_FROM_END")
                .build();

        return soapClient.sendSoapRequest(
                request,
                GetReceiptsTapeResponse.class,
                "GetReceiptsTapeRequest"
        );
    }

    public void getAllReceipts() {
        String marker = "S_FROM_END";
        boolean hasMore = true;

        while (hasMore) {
            GetReceiptsTapeResponse response = getReceipts(marker);

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                log.info("Получено чеков: {}", response.getReceipts().size());

                response.getReceipts().forEach(receipt -> {
                    log.info("Чек от: {}, источник: {}",
                            receipt.getUserIdentifier(),
                            receipt.getSourceCode());
                });
            }

            marker = response.getNextMarker();
            hasMore = response.getTotalExpectedRemainingPolls() != null &&
                    response.getTotalExpectedRemainingPolls() > 1;
        }
    }

    public PostBindPartnerResponse bindUserSync(String phoneNumber, String requestId) {
        log.info("Синхронное подключение пользователя: {}", phoneNumber);

        // ←←← ВОТ СЮДА, ДО ОТПРАВКИ!
        log.info(">>> Формируем PostBindPartnerRequest: requestId = {}, userIdentifier = {}",
                requestId, phoneNumber);

        // Отправляем запрос
        SendMessageResponse messageResponse = bindUser(phoneNumber, requestId);

        log.info("Получен MessageId: {}, начинаем опрос результата...", messageResponse.getMessageId());

        try {
            PostBindPartnerResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    PostBindPartnerResponse.class
            );

            log.info("Заявка успешно обработана! Ответ: {}", response);
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        } catch (RuntimeException e) {
            log.error("Ошибка обработки заявки на стороне ФНС: {}", e.getMessage());
            throw e;
        }
    }

    // ============================================
// МЕТОДЫ ДЛЯ РАБОТЫ С ЧЕКАМИ
// ============================================

    /**
     * СИНХРОННОЕ получение ленты чеков
     * Отправляет запрос и автоматически опрашивает результат
     *
     * @param marker маркер для получения чеков (S_FROM_END, S_FROM_BEGINNING, или NextMarker)
     * @return ответ с чеками
     */
    public GetReceiptsTapeResponse getReceiptsSync(String marker) {
        log.info(">>> Синхронное получение ленты чеков с маркером: {}", marker);

        // Создаем внутренний запрос
        GetReceiptsTapeRequest innerRequest = GetReceiptsTapeRequest.builder()
                .marker(marker != null ? marker : "S_FROM_END")
                .build();

        // Оборачиваем в SendMessageRequest (асинхронный механизм)
        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        // Отправляем асинхронный запрос
        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            // Опрашиваем результат (автоматическое ожидание)
            GetReceiptsTapeResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    GetReceiptsTapeResponse.class
            );

            int receiptsCount = response.getReceipts() != null ? response.getReceipts().size() : 0;
            log.info("✅ Получено чеков: {}", receiptsCount);

            if (response.getNextMarker() != null) {
                log.debug("NextMarker: {}", response.getNextMarker());
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Прервано ожидание результата", e);
            throw new RuntimeException("Прервано ожидание результата", e);
        } catch (RuntimeException e) {
            log.error("❌ Ошибка получения чеков: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Получение ВСЕХ доступных чеков с автоматической пагинацией
     * Проходит по всем порциям чеков используя NextMarker
     */
    public void getAllReceiptsSync() {
        log.info("=== НАЧАЛО ПОЛУЧЕНИЯ ВСЕХ ЧЕКОВ ===");

        String marker = "S_FROM_END";
        int totalReceipts = 0;
        int iteration = 0;
        boolean hasMore = true;
        int maxIterations = 50; // Защита от бесконечного цикла

        while (hasMore && iteration < maxIterations) {
            iteration++;
            log.info("--- Итерация {} ---", iteration);

            try {
                GetReceiptsTapeResponse response = getReceiptsSync(marker);

                // Обрабатываем полученные чеки
                if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                    int batchSize = response.getReceipts().size();
                    totalReceipts += batchSize;
                    log.info("📦 Получено чеков в этой порции: {}", batchSize);

                    // Выводим информацию о каждом чеке
                    response.getReceipts().forEach(receipt -> {
                        log.info("  📄 Чек:");
                        log.info("     - Пользователь: {}", receipt.getUserIdentifier());
                        log.info("     - Телефон: {}", receipt.getPhone());
                        log.info("     - Email: {}", receipt.getEmail());
                        log.info("     - Дата: {}", receipt.getReceiveDate());
                        log.info("     - Источник: {}", receipt.getSourceCode());

                        // Если есть JSON чека, можно его декодировать
                        if (receipt.getJson() != null && receipt.getJson().length > 0) {
                            try {
                                String jsonContent = new String(receipt.getJson(), "UTF-8");
                                log.debug("     - JSON: {}", jsonContent.substring(0, Math.min(100, jsonContent.length())) + "...");
                            } catch (Exception e) {
                                log.warn("     - ⚠️ Не удалось декодировать JSON чека", e);
                            }
                        }
                    });
                } else {
                    log.info("📭 Чеков в этой порции нет");
                }

                // Проверяем наличие следующего маркера
                if (response.getNextMarker() != null && !response.getNextMarker().isEmpty()) {
                    marker = response.getNextMarker();
                    log.debug("➡️ NextMarker для следующей итерации: {}", marker);
                } else {
                    log.info("⏹️ NextMarker отсутствует - это была последняя порция");
                    hasMore = false;
                }

                // Проверяем условие продолжения
                Long remainingPolls = response.getTotalExpectedRemainingPolls();
                if (remainingPolls != null) {
                    log.info("📊 Осталось порций для загрузки: {}", remainingPolls);
                    hasMore = hasMore && (remainingPolls > 0);
                }

                // Небольшая пауза между запросами (для снижения нагрузки)
                if (hasMore) {
                    Thread.sleep(500);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("❌ Прервано получение чеков на итерации {}", iteration);
                break;
            } catch (Exception e) {
                log.error("❌ Ошибка при получении чеков на итерации {}", iteration, e);
                break;
            }
        }

        if (iteration >= maxIterations) {
            log.warn("⚠️ Достигнуто максимальное количество итераций ({}) - остановка", maxIterations);
        }

        log.info("=== ЗАВЕРШЕНО: Всего получено {} чеков за {} итераций ===",
                totalReceipts, iteration);
    }

    /**
     * ТЕСТОВЫЙ метод для демонстрации работы с чеками
     * Выводит подробную информацию о процессе
     */
    public void testReceiptsFlow() {
        log.info("╔════════════════════════════════════════════════════╗");
        log.info("║   ТЕСТИРОВАНИЕ ПОЛУЧЕНИЯ ЧЕКОВ                     ║");
        log.info("╚════════════════════════════════════════════════════╝");

        try {
            log.info("");
            log.info("ШАГ 1: Получаем последние чеки (маркер S_FROM_END)");
            log.info("─────────────────────────────────────────────────────");

            GetReceiptsTapeResponse response = getReceiptsSync("S_FROM_END");

            if (response.getReceipts() == null || response.getReceipts().isEmpty()) {
                log.warn("");
                log.warn("╔════════════════════════════════════════════════════╗");
                log.warn("║   ⚠️ ЧЕКОВ НЕ НАЙДЕНО                              ║");
                log.warn("╚════════════════════════════════════════════════════╝");
                log.warn("");
                log.warn("ВОЗМОЖНЫЕ ПРИЧИНЫ:");
                log.warn("  1. ❌ У вас нет подключенных пользователей");
                log.warn("  2. ❌ Подключенные пользователи не сканировали чеки");
                log.warn("  3. ❌ С момента последнего сканирования прошло > 5 дней");
                log.warn("");
                log.warn("ЧТО ДЕЛАТЬ:");
                log.warn("  1. Подключите тестового пользователя:");
                log.warn("     POST http://localhost:8085/api/mco/bind-user-test");
                log.warn("");
                log.warn("  2. Зайдите в ЛК МЧО и одобрите заявку:");
                log.warn("     https://dr.stm-labs.ru/partners");
                log.warn("");
                log.warn("  3. Отсканируйте чек через мобильное приложение МЧО");
                log.warn("");
                log.warn("  4. Подождите 2-3 минуты и повторите запрос");
                log.warn("");

            } else {
                log.info("");
                log.info("╔════════════════════════════════════════════════════╗");
                log.info("║   ✅ УСПЕХ! ЧЕКИ ПОЛУЧЕНЫ                          ║");
                log.info("╚════════════════════════════════════════════════════╝");
                log.info("");
                log.info("📊 СТАТИСТИКА:");
                log.info("   • Получено чеков: {}", response.getReceipts().size());
                log.info("   • Осталось порций: {}", response.getTotalExpectedRemainingPolls());
                log.info("");

                // Детали первого чека
                var firstReceipt = response.getReceipts().get(0);
                log.info("📄 ДЕТАЛИ ПЕРВОГО ЧЕКА:");
                log.info("   • Пользователь: {}", firstReceipt.getUserIdentifier());
                log.info("   • Телефон: {}", firstReceipt.getPhone());
                log.info("   • Email: {}", firstReceipt.getEmail());
                log.info("   • Дата: {}", firstReceipt.getReceiveDate());
                log.info("   • Источник: {}", firstReceipt.getSourceCode());
                log.info("");

                // Информация о JSON
                if (firstReceipt.getJson() != null && firstReceipt.getJson().length > 0) {
                    try {
                        String jsonContent = new String(firstReceipt.getJson(), "UTF-8");
                        log.info("   • Размер JSON: {} байт", firstReceipt.getJson().length);
                        log.info("   • Превью JSON: {}...",
                                jsonContent.substring(0, Math.min(200, jsonContent.length())));
                    } catch (Exception e) {
                        log.warn("   • ⚠️ Ошибка декодирования JSON");
                    }
                }

                log.info("");
                log.info("🔗 СЛЕДУЮЩИЙ МАРКЕР:");
                log.info("   • NextMarker: {}", response.getNextMarker());
                log.info("");
                log.info("💡 СОВЕТ: Используйте NextMarker для получения следующей порции:");
                log.info("   GET /api/mco/receipts?marker={}", response.getNextMarker());
                log.info("");
            }

        } catch (Exception e) {
            log.error("");
            log.error("╔════════════════════════════════════════════════════╗");
            log.error("║   ❌ ОШИБКА ПРИ ТЕСТИРОВАНИИ                       ║");
            log.error("╚════════════════════════════════════════════════════╝");
            log.error("");
            log.error("Ошибка: {}", e.getMessage());
            log.error("");
            log.error("ПОДРОБНОСТИ:");
            log.error("", e);
            log.error("");
        }
    }
}