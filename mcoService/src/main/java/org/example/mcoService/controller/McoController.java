package org.example.mcoService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.api.ApiResponse;
import org.example.mcoService.dto.api.BindRequestStatusDto;
import org.example.mcoService.dto.api.CreateBindRequestDto;
import org.example.mcoService.dto.api.ReceiptsResponseDto;
import org.example.mcoService.dto.response.GetBindPartnerStatusResponse;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.service.McoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/mco")
@RequiredArgsConstructor
public class McoController {

    private final McoService mcoService;
    private final McoProperties mcoProperties;
    /**
     * Регистрация партнера в системе МЧО
     * POST /api/mco/register?logoPath=/path/to/logo.jpg
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> registerPartner(
            @RequestParam(required = false) String logoPath) {

        try {
            log.info("Регистрация партнера в МЧО");
            String partnerId = mcoService.initializePartner(logoPath);

            return ResponseEntity.ok(ApiResponse.success(
                    "Партнер успешно зарегистрирован в системе МЧО. ID: " + partnerId,
                    null
            ));

        } catch (Exception e) {
            log.error("Ошибка регистрации партнера", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка регистрации партнера: " + e.getMessage())
            );
        }
    }

    /**
     * Подключение пользователя к партнеру
     * POST /api/mco/bind-user?phone=79999999999&permissionGroups=DEFAULT
     */
    @PostMapping("/bind-user")
    public ResponseEntity<ApiResponse<CreateBindRequestDto>> bindUser(
            @RequestParam String phone,
            @RequestParam(required = false, defaultValue = "DEFAULT") String permissionGroups) {

        try {
            log.info("Подключение пользователя: {}", phone);

            String requestId = mcoService.connectUser(phone);

            CreateBindRequestDto data = CreateBindRequestDto.builder()
                    .requestId(requestId)
                    .userIdentifier(phone)
                    .permissionGroups(permissionGroups)
                    .statusCheckUrl("/api/mco/bind-request-status?requestId=" + requestId)
                    .userInstruction("Пользователю отправлена заявка в ЛК МЧО. " +
                            "Для активации подключения пользователь должен одобрить заявку на сайте https://dr.stm-labs.ru/")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(
                    "Заявка на подключение создана успешно",
                    data
            ));

        } catch (Exception e) {
            log.error("Ошибка подключения пользователя", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка подключения пользователя: " + e.getMessage())
            );
        }
    }

    /**
     * Подключение тестового пользователя (фиксированный номер)
     * POST /api/mco/bind-user-test
     */
    @PostMapping("/bind-user-test")
    public ResponseEntity<ApiResponse<CreateBindRequestDto>> bindUserTest() {
        String testPhone = "79054455906";
        return bindUser(testPhone, "DEFAULT");
    }

    // ==========================================
    // СТАТУСЫ ЗАЯВОК
    // ==========================================

    /**
     * Проверка статуса одной заявки
     * GET /api/mco/bind-request-status?requestId=YOUR_REQUEST_ID
     */
    @GetMapping("/bind-request-status")
    public ResponseEntity<ApiResponse<BindRequestStatusDto>> getBindRequestStatus(
            @RequestParam String requestId) {

        try {
            log.info("Проверка статуса заявки: {}", requestId);

            GetBindPartnerStatusResponse.BindPartnerStatus status =
                    mcoService.checkBindRequestStatus(requestId);

            BindRequestStatusDto data = BindRequestStatusDto.fromMcoResponse(status);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (RuntimeException e) {
            log.warn("Статус не найден для заявки: {}", requestId);

            // Если статус не найден - возможно заявка еще не обработана
            if (e.getMessage().contains("Не получен статус")) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Заявка еще не обработана пользователем",
                        BindRequestStatusDto.builder()
                                .requestId(requestId)
                                .status("PENDING")
                                .statusDescription("Заявка отправлена, ожидает обработки пользователем")
                                .build()
                ));
            }

            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка проверки статуса: " + e.getMessage())
            );
        }
    }

    /**
     * Проверка статусов нескольких заявок
     * POST /api/mco/bind-requests-status
     * Body (JSON): ["REQUEST_ID_1", "REQUEST_ID_2", "REQUEST_ID_3"]
     */
    @PostMapping("/bind-requests-status")
    public ResponseEntity<ApiResponse<List<BindRequestStatusDto>>> getBindRequestsStatus(
            @RequestBody List<String> requestIds) {

        try {
            log.info("Проверка статусов заявок, количество: {}", requestIds.size());

            if (requestIds.size() > 50) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Максимум 50 requestIds за один запрос")
                );
            }

            List<GetBindPartnerStatusResponse.BindPartnerStatus> statuses =
                    mcoService.checkBindRequestStatuses(requestIds);

            List<BindRequestStatusDto> data = statuses.stream()
                    .map(BindRequestStatusDto::fromMcoResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("Ошибка проверки статусов заявок", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка проверки статусов: " + e.getMessage())
            );
        }
    }

    // ==========================================
    // РАБОТА С ЧЕКАМИ
    // ==========================================

    /**
     * Тестовое получение чеков (одна порция)
     * GET /api/mco/receipts/test
     */
    @GetMapping("/receipts/test")
    public ResponseEntity<ApiResponse<ReceiptsResponseDto>> testReceipts() {
        try {
            log.info(">>> ЗАПУСК ТЕСТОВОГО ПОЛУЧЕНИЯ ЧЕКОВ <<<");

            int receiptsCount = mcoService.testReceiptsOnce();

            if (receiptsCount == 0) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Чеков не найдено. Убедитесь что пользователь подключен и отсканировал чеки в приложении МЧО.",
                        ReceiptsResponseDto.builder()
                                .totalCount(0)
                                .receipts(List.of())
                                .info("Нет подключенных пользователей или нет отсканированных чеков")
                                .build()
                ));
            }

            return ResponseEntity.ok(ApiResponse.success(
                    "Чеки успешно получены. Подробности в логах.",
                    ReceiptsResponseDto.builder()
                            .totalCount(receiptsCount)
                            .info("Для получения детальной информации смотрите логи приложения")
                            .build()
            ));

        } catch (Exception e) {
            log.error("Ошибка тестирования получения чеков", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка получения чеков: " + e.getMessage())
            );
        }
    }

    /**
     * Получение чеков по маркеру
     * GET /api/mco/receipts?marker=S_FROM_END
     */
    @GetMapping("/receipts")
    public ResponseEntity<ApiResponse<ReceiptsResponseDto>> getReceiptsByMarker(
            @RequestParam(defaultValue = "S_FROM_END") String marker) {

        try {
            GetReceiptsTapeResponse response = mcoService.getReceiptsByMarker(marker);

            List<ReceiptsResponseDto.ReceiptDto> receipts = null;
            if (response.getReceipts() != null) {
                receipts = response.getReceipts().stream()
                        .map(r -> ReceiptsResponseDto.ReceiptDto.builder()
                                .userIdentifier(r.getUserIdentifier())
                                .receiveDate(r.getReceiveDate())
                                .sourceCode(r.getSourceCode())
                                .phone(r.getPhone())
                                .email(r.getEmail())
                                .json(r.getJson())
                                .build())
                        .collect(Collectors.toList());
            }

            ReceiptsResponseDto data = ReceiptsResponseDto.builder()
                    .receipts(receipts != null ? receipts : List.of())
                    .totalCount(receipts != null ? receipts.size() : 0)
                    .nextMarker(response.getNextMarker())
                    .remainingPolls(response.getTotalExpectedRemainingPolls())
                    .build();

            String message = receipts != null && !receipts.isEmpty()
                    ? "Получена порция чеков"
                    : "В этой порции чеков нет";

            return ResponseEntity.ok(ApiResponse.success(message, data));

        } catch (Exception e) {
            log.error("Ошибка получения чеков по маркеру: {}", marker, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка получения чеков: " + e.getMessage())
            );
        }
    }

    /**
     * Полная синхронизация чеков
     * GET /api/mco/receipts/sync
     */
    @GetMapping("/receipts/sync")
    public ResponseEntity<ApiResponse<Object>> syncReceipts() {
        try {
            log.info(">>> ЗАПУСК ПОЛНОЙ СИНХРОНИЗАЦИИ ЧЕКОВ <<<");

            mcoService.syncReceipts();

            return ResponseEntity.ok(ApiResponse.success(
                    "Полная синхронизация завершена успешно. Подробности в логах.",
                    null
            ));

        } catch (Exception e) {
            log.error("Ошибка синхронизации чеков", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка синхронизации: " + e.getMessage())
            );
        }
    }

    /**
     * Детальный тест получения чеков
     * GET /api/mco/receipts/test-detailed
     */
    @GetMapping("/receipts/test-detailed")
    public ResponseEntity<ApiResponse<Object>> testReceiptsDetailed() {
        try {
            mcoService.detailedReceiptsTest();
            return ResponseEntity.ok(ApiResponse.success(
                    "Детальный тест завершен. Смотрите логи для подробностей.",
                    null
            ));
        } catch (Exception e) {
            log.error("Ошибка детального теста", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка детального теста: " + e.getMessage())
            );
        }
    }

    /**
     * Получение статистики по чекам
     * GET /api/mco/receipts/stats
     */
    @GetMapping("/receipts/stats")
    public ResponseEntity<ApiResponse<Object>> getReceiptsStats() {
        try {
            String stats = mcoService.getReceiptsStats();
            return ResponseEntity.ok(ApiResponse.success(stats, null));
        } catch (Exception e) {
            log.error("Ошибка получения статистики", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка получения статистики: " + e.getMessage())
            );
        }
    }

    // ==========================================
    // СЛУЖЕБНЫЕ ЭНДПОИНТЫ
    // ==========================================

    /**
     * Health check
     * GET /api/mco/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "МЧО Сервис работает корректно",
                null
        ));
    }

    /**
     * Диагностика подключения
     * GET /api/mco/diagnose
     */
    @GetMapping("/diagnose")
    public ResponseEntity<ApiResponse<Object>> diagnose() {
        try {
            boolean tokenPresent = mcoProperties.getApi().getToken() != null;
            boolean userTokenPresent = mcoProperties.getApi().getUserToken() != null;

            if (!tokenPresent || !userTokenPresent) {
                return ResponseEntity.ok(ApiResponse.error(
                        "Токены не настроены",
                        "MISSING_TOKENS",
                        "Token present: " + tokenPresent + ", UserToken present: " + userTokenPresent
                ));
            }

            return ResponseEntity.ok(ApiResponse.success(
                    "Конфигурация корректна. API URL: " + mcoProperties.getApi().getBaseUrl(),
                    null
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка диагностики: " + e.getMessage())
            );
        }
    }
}