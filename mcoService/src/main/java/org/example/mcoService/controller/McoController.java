package org.example.mcoService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.api.*;
import org.example.mcoService.dto.response.*;
import org.example.mcoService.exception.BusinessMcoException;
import org.example.mcoService.exception.FatalMcoException;
import org.example.mcoService.exception.McoErrorCode;
import org.example.mcoService.exception.RetryableMcoException;
import org.example.mcoService.service.BindApprovalPollingService;
import org.example.mcoService.service.McoService;
import org.example.mcoService.service.ReceiptService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/mco")
@RequiredArgsConstructor
public class McoController {

    private final McoService mcoService;
    private final ReceiptService receiptService;
    private final BindApprovalPollingService pollingService;

    @GetMapping("/receipts/user")
    public ResponseEntity<ApiResponse<Page<ReceiptDto>>> getUserReceipts(
            @RequestParam String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("receiptDateTime").descending());

            Page<ReceiptDto> receiptsPage = receiptService.getUserReceiptsByPhone(phone, pageable);

            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Найдено чеков: %d (страница %d из %d)",
                            receiptsPage.getTotalElements(),
                            receiptsPage.getNumber(),
                            receiptsPage.getTotalPages()),
                    receiptsPage
            ));

        } catch (Exception e) {
            log.error("Ошибка получения чеков пользователя {}", phone, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка: " + e.getMessage())
            );
        }
    }

    @PostMapping("/bind-users-batch")
    public ResponseEntity<ApiResponse<Object>> bindUsersBatch(
            @RequestBody List<String> phoneNumbers) {

        try {
            if (phoneNumbers == null || phoneNumbers.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Список телефонов не может быть пустым")
                );
            }

            log.info("Пакетное подключение {} пользователей", phoneNumbers.size());

            String requestId = UUID.randomUUID().toString();
            PostBindPartnerBatchResponse response = mcoService.bindUsersBatch(requestId, phoneNumbers);

            Map<String, Object> data = new HashMap<>();
            data.put("requestId", response.getRequestId());
            data.put("acceptedCount", response.getAcceptedUserIdentifiers() != null ?
                    response.getAcceptedUserIdentifiers().size() : 0);
            data.put("rejectedCount", response.getRejectedUserIdentifiers() != null ?
                    response.getRejectedUserIdentifiers().size() : 0);
            data.put("acceptedUsers", response.getAcceptedUserIdentifiers());
            data.put("rejectedUsers", response.getRejectedUserIdentifiers());
            data.put("statusCheckUrl", "/api/mco/bind-request-status?requestId=" + requestId);

            String message = response.getAcceptedUserIdentifiers() != null && !response.getAcceptedUserIdentifiers().isEmpty()
                    ? "Пакетная заявка отправлена, часть пользователей принята"
                    : "Пакетная заявка отправлена, но никто не принят (проверьте отклонённых)";

            return ResponseEntity.ok(ApiResponse.success(message, data));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.BATCH_TOO_LARGE.equals(e.getErrorCode()) ||
                    McoErrorCode.BATCH_TOO_MANY_INVALID_IDENTIFIERS.equals(e.getErrorCode())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Слишком большой пакет или слишком много некорректных номеров"
                        )
                );
            }

            if (McoErrorCode.BATCH_REQUEST_ID_DUPLICATE.equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Заявка с таким requestId уже существует"
                        )
                );
            }

            log.info("Бизнес-ошибка при пакетной привязке: {} - {}",
                    e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Ошибка валидации пакетной заявки"
                    )
            );

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при пакетной привязке: {}", e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите пакетную отправку позже"
                    ));

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при пакетной привязке: {} - {}",
                    e.getErrorCode().getCode(), e.getErrorMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера при пакетной привязке"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при пакетном подключении пользователей", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при пакетной привязке")
            );
        }
    }

    @GetMapping("/bind-events")
    public ResponseEntity<ApiResponse<Object>> getBindEvents(
            @RequestParam(required = false, defaultValue = "S_FROM_END") String marker) {

        try {
            log.info("Запрос событий с маркером: {}", marker);

            GetBindPartnerEventResponse response = mcoService.getBindPartnerEvents(marker);

            Map<String, Object> data = new HashMap<>();
            data.put("events", response.getEvents());
            data.put("nextMarker", response.getMarker());
            data.put("eventsCount", response.getEvents() != null ? response.getEvents().size() : 0);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при получении событий: {}", e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Повторите запрос позже"
                    ));
        } catch (BusinessMcoException e) {
            log.info("Бизнес-ошибка при получении событий: {}", e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Некорректный запрос"
                    ));
        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при получении событий: {}", e.getErrorCode().getCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера"
                    ));
        } catch (Exception e) {
            log.error("Необработанная ошибка при получении событий", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка сервера")
            );
        }
    }

    @PostMapping("/send-notification")
    public ResponseEntity<ApiResponse<Object>> sendNotification(
            @RequestBody SendNotificationDto dto) {

        try {
            if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Номер телефона обязателен")
                );
            }
            if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Заголовок обязателен")
                );
            }
            if (dto.getMessage() == null || dto.getMessage().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Сообщение обязательно")
                );
            }
            if (dto.getShortMessage() == null || dto.getShortMessage().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Короткое сообщение обязательно")
                );
            }

            String category = dto.getCategory() != null ? dto.getCategory() : "GENERAL";
            if (!category.equals("GENERAL") && !category.equals("CASHBACK")) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Категория должна быть GENERAL или CASHBACK")
                );
            }

            log.info("Отправка уведомления пользователю: {}", dto.getPhoneNumber());

            String requestId = UUID.randomUUID().toString();
            PostNotificationResponse response = mcoService.sendNotification(
                    requestId,
                    dto.getPhoneNumber(),
                    dto.getTitle(),
                    dto.getMessage(),
                    dto.getShortMessage(),
                    category,
                    dto.getExternalItemId(),
                    dto.getExternalItemUrl()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("requestId", response.getRequestId());
            data.put("handledAt", response.getHandledAt());
            data.put("phoneNumber", dto.getPhoneNumber());

            return ResponseEntity.ok(ApiResponse.success(
                    "Уведомление успешно отправлено",
                    data
            ));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.USER_IDENTIFIER_NOT_FOUND.equals(e.getErrorCode()) ||
                    McoErrorCode.USER_IDENTIFIER_UNBOUND.equals(e.getErrorCode()) ||
                    McoErrorCode.IDENTIFIER_UNBOUND.equals(e.getErrorCode())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Пользователь не найден или не привязан к партнёру — уведомление невозможно отправить"
                        )
                );
            }

            if (McoErrorCode.NOTIFICATION_PERMISSION_DENIED.equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Пользователь запретил получение уведомлений от данного партнёра"
                        )
                );
            }

            if (McoErrorCode.DUPLICATE_NOTIFICATION.equals(e.getErrorCode()) ||
                    McoErrorCode.NOTIFICATION_RATE_LIMIT_EXCEEDED.equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Повторная отправка или превышен лимит уведомлений — попробуйте позже"
                        )
                );
            }

            log.info("Бизнес-ошибка при отправке уведомления пользователю {}: {} - {}",
                    dto.getPhoneNumber(), e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Ошибка валидации или бизнес-правил при отправке уведомления"
                    )
            );

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при отправке уведомления пользователю {}: {}",
                    dto.getPhoneNumber(), e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите отправку позже"
                    ));

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при отправке уведомления пользователю {}: {} - {}",
                    dto.getPhoneNumber(), e.getErrorCode().getCode(), e.getErrorMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера при отправке уведомления"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при отправке уведомления пользователю {}", dto.getPhoneNumber(), e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при отправке уведомления")
            );
        }
    }

    @GetMapping("/unbound-users")
    public ResponseEntity<ApiResponse<Object>> getUnboundUsers(
            @RequestParam(required = false, defaultValue = "S_FROM_END") String marker) {

        try {
            log.info("Запрос отключившихся пользователей с маркером: {}", marker);

            GetUnboundPartnerResponse response = mcoService.getUnboundPartners(marker);

            Map<String, Object> data = new HashMap<>();
            data.put("unboundUsers", response.getUnbounds());
            data.put("nextMarker", response.getNextMarker());
            data.put("hasMore", response.getHasMore());
            data.put("count", response.getUnbounds() != null ? response.getUnbounds().size() : 0);

            String message = response.getHasMore()
                    ? "Получены отключившиеся пользователи (есть еще данные)"
                    : "Получены все отключившиеся пользователи";

            return ResponseEntity.ok(ApiResponse.success(message, data));

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при получении списка отключившихся (marker: {}): {} - {}",
                    marker, e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите запрос позже"
                    ));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.MARKER_INVALID_UNBOUND.equals(e.getErrorCode()) ||
                    McoErrorCode.MARKER_INVALID.equals(e.getErrorCode()) ||
                    McoErrorCode.NO_UNBOUND_USERS.equals(e.getErrorCode())) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Нет отключившихся пользователей или некорректный маркер",
                        Map.of(
                                "unboundUsers", List.of(),
                                "nextMarker", null,
                                "hasMore", false,
                                "count", 0
                        )
                ));
            }

            log.info("Бизнес-ошибка при получении отключившихся пользователей (marker: {}): {} - {}",
                    marker, e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Некорректный запрос списка отключившихся пользователей"
                    )
            );

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при получении отключившихся пользователей (marker: {}): {} - {}",
                    marker, e.getErrorCode().getCode(), e.getErrorMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при получении отключившихся пользователей (marker: {})", marker, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при получении списка отключившихся пользователей")
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> registerPartner(
            @RequestParam(required = false) String logoPath) {

        try {
            log.info("Регистрация партнера в МЧО");
            String partnerId = mcoService.initializePartner(logoPath);

            return ResponseEntity.ok(ApiResponse.success(
                    "Партнер успешно зарегистрирован в системе МЧО. ID: " + partnerId,
                    Map.of("partnerId", partnerId)
            ));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.PARTNER_ALREADY_REGISTERED.equals(e.getErrorCode()) ||
                    McoErrorCode.INN_ALREADY_USED.equals(e.getErrorCode())) {
                log.info("Партнер уже зарегистрирован: {}", e.getErrorMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Партнёр с таким ИНН уже зарегистрирован в системе"
                        )
                );
            }

            log.info("Бизнес-ошибка при регистрации партнера: {} - {}",
                    e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Некорректные данные для регистрации партнёра"
                    )
            );

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при регистрации партнера: {}", e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите попытку позже"
                    ));

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при регистрации партнера: {} - {}",
                    e.getErrorCode().getCode(), e.getErrorMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера при регистрации"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при регистрации партнера", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при регистрации партнёра")
            );
        }
    }

    @PostMapping("/bind-user")
    public ResponseEntity<ApiResponse<CreateBindRequestDto>> bindUser(
            @RequestParam String phone,
            @RequestParam(required = false, defaultValue = "DEFAULT") String permissionGroups) {

        try {
            log.info("Подключение пользователя по телефону: {}", phone);

            String requestId = mcoService.connectUser(phone);

            pollingService.startPollingAndUpdateOnApproval(requestId, phone);

            CreateBindRequestDto data = CreateBindRequestDto.builder()
                    .requestId(requestId)
                    .userIdentifier(phone)
                    .permissionGroups(permissionGroups)
                    .statusCheckUrl("/api/mco/bind-request-status?requestId=" + requestId)
                    .userInstruction("Пользователю отправлена заявка в ЛК МЧО. Для активации подключения пользователь должен одобрить заявку на сайте https://dr.stm-labs.ru/")
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Заявка на подключение создана успешно", data));

        } catch (Exception e) {
            log.error("Ошибка при создании заявки на подключение", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Ошибка подключения: " + e.getMessage())
            );
        }
    }

    @PostMapping("/unbind-user")
    public ResponseEntity<ApiResponse<UnbindUserResponse>> unbindUser(
            @RequestBody UnbindUserRequest request) {

        try {
            log.info("Запрос на отключение пользователя: {}", request.getPhoneNumber());

            mcoService.unbindUser(request.getPhoneNumber(), request.getUnbindReason());

            UnbindUserResponse response = UnbindUserResponse.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .status("UNBOUND")
                    .unboundAt(java.time.LocalDateTime.now())
                    .message("Пользователь успешно отключен от партнера")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(
                    "Пользователь успешно отключен",
                    response
            ));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.IDENTIFIER_UNBOUND.equals(e.getErrorCode()) ||
                    McoErrorCode.USER_NOT_BOUND.equals(e.getErrorCode())) {
                log.info("Пользователь уже отключен: {}", request.getPhoneNumber());
                return ResponseEntity.ok(ApiResponse.success(
                        "Пользователь уже отключен от партнера",
                        UnbindUserResponse.builder()
                                .phoneNumber(request.getPhoneNumber())
                                .status("ALREADY_UNBOUND")
                                .unboundAt(java.time.LocalDateTime.now())
                                .message("Отключение не требуется — пользователь уже не привязан")
                                .build()
                ));
            }

            log.info("Бизнес-ошибка при отключении: {} - {}", e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Ошибка валидации запроса на отключение"
                    )
            );

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при отключении: {}", e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите позже"
                    ));

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при отключении пользователя {}: {}",
                    request.getPhoneNumber(), e.getErrorCode().getCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при отключении пользователя {}",
                    request.getPhoneNumber(), e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка сервера")
            );
        }
    }

    @GetMapping("/bind-request-status")
    public ResponseEntity<ApiResponse<BindRequestStatusDto>> getBindRequestStatus(
            @RequestParam String requestId) {

        try {
            log.info("Проверка статуса заявки: {}", requestId);

            GetBindPartnerStatusResponse.BindPartnerStatus status =
                    mcoService.checkBindRequestStatus(requestId);

            BindRequestStatusDto data = BindRequestStatusDto.fromMcoResponse(status);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.REQUEST_NOT_FOUND.equals(e.getErrorCode()) ||
                    McoErrorCode.REQUEST_VALIDATION_ERROR.equals(e.getErrorCode()) ||
                    e.getErrorCode().getCode().contains("REQUEST_NOT_FOUND") ||
                    e.getErrorCode().getCode().contains("NOT_FOUND")) {

                log.info("Заявка не найдена или ещё не обработана: {}", requestId);
                return ResponseEntity.ok(ApiResponse.success(
                        BindRequestStatusDto.builder()
                                .requestId(requestId)
                                .status("NOT_FOUND")
                                .statusDescription("Заявка не найдена или ещё не создана/обработана пользователем")
                                .build()
                ));
            }

            log.info("Бизнес-ошибка при проверке статуса заявки {}: {} - {}",
                    requestId, e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Некорректный запрос на проверку статуса"
                    )
            );

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при проверке статуса заявки {}: {}",
                    requestId, e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите позже"
                    ));

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при проверке статуса заявки {}: {}",
                    requestId, e.getErrorCode().getCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при проверке статуса заявки {}", requestId, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при проверке статуса")
            );
        }
    }

    @PostMapping("/bind-requests-status")
    public ResponseEntity<ApiResponse<List<BindRequestStatusDto>>> getBindRequestsStatus(
            @RequestBody List<String> requestIds) {

        try {
            log.info("Проверка статусов заявок, количество: {}", requestIds.size());

            if (requestIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Список requestIds не может быть пустым")
                );
            }

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

        } catch (BusinessMcoException e) {
            if (McoErrorCode.TOO_MANY_REQUEST_IDS.equals(e.getErrorCode()) ||
                    McoErrorCode.INVALID_REQUEST_ID_FORMAT.equals(e.getErrorCode()) ||
                    McoErrorCode.REQUEST_VALIDATION_ERROR.equals(e.getErrorCode())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Некорректный список requestIds"
                        )
                );
            }

            if (McoErrorCode.PARTIAL_REQUESTS_NOT_FOUND.equals(e.getErrorCode()) ||
                    McoErrorCode.REQUEST_NOT_FOUND.equals(e.getErrorCode())) {
                // Возвращаем частичный результат + информацию об ошибке
                log.warn("Частичная ошибка при получении статусов: {}", e.getErrorMessage());
                // Здесь можно продолжить и вернуть то, что есть, но для простоты возвращаем ошибку
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                e.getErrorMessage() + " (некоторые заявки не найдены)",
                                e.getErrorCode().getCode(),
                                "Не все заявки найдены"
                        )
                );
            }

            log.info("Бизнес-ошибка при массовой проверке статусов: {} - {}",
                    e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Ошибка проверки статусов заявок"
                    )
            );

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при массовой проверке статусов: {}", e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите запрос позже"
                    ));

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при массовой проверке статусов: {} - {}",
                    e.getErrorCode().getCode(), e.getErrorMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при массовой проверке статусов заявок", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при проверке статусов")
            );
        }
    }

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

        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при получении ленты чеков (marker: {}): {} - {}",
                    marker, e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите запрос через несколько секунд"
                    ));

        } catch (BusinessMcoException e) {
            if (McoErrorCode.RECEIPT_TAPE_BAD_MARKER.equals(e.getErrorCode()) ||
                    McoErrorCode.RECEIPT_TAPE_MARKER_INVALID.equals(e.getErrorCode()) ||
                    McoErrorCode.RECEIPT_TAPE_NO_ACCESS.equals(e.getErrorCode())) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                e.getErrorMessage(),
                                e.getErrorCode().getCode(),
                                "Некорректный маркер или нет доступа к ленте чеков"
                        )
                );
            }

            log.info("Бизнес-ошибка при получении чеков (marker: {}): {} - {}",
                    marker, e.getErrorCode().getCode(), e.getErrorMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Ошибка запроса ленты чеков"
                    )
            );

        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при получении чеков по маркеру {}: {} - {}",
                    marker, e.getErrorCode().getCode(), e.getErrorMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера при получении чеков"
                    ));

        } catch (Exception e) {
            log.error("Необработанная ошибка при получении чеков по маркеру: {}", marker, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("Внутренняя ошибка при получении чеков")
            );
        }
    }

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

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                "МЧО Сервис работает корректно",
                null
        ));
    }
}