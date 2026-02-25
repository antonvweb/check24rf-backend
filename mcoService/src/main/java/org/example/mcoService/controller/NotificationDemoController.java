package org.example.mcoService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.api.ApiResponse;
import org.example.mcoService.dto.notification.NotificationParams;
import org.example.mcoService.dto.response.PostNotificationResponse;
import org.example.mcoService.enums.NotificationType;
import org.example.mcoService.exception.BusinessMcoException;
import org.example.mcoService.exception.FatalMcoException;
import org.example.mcoService.exception.McoErrorCode;
import org.example.mcoService.exception.RetryableMcoException;
import org.example.mcoService.service.McoService;
import org.example.mcoService.service.NotificationTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Демо-контроллер для демонстрации системы уведомлений команде МЧО.
 * Показывает все типы GENERAL уведомлений и их отправку.
 *
 * Путь: /api/mco/notifications/demo
 */
@Slf4j
@RestController
@RequestMapping("/api/mco/notifications/demo")
@RequiredArgsConstructor
public class NotificationDemoController {

    private final McoService mcoService;
    private final NotificationTemplateService templateService;

    /**
     * Отправка уведомления о новых чеках.
     * Демонстрирует уведомление NEW_RECEIPTS_AVAILABLE.
     *
     * @param phoneNumber номер телефона пользователя
     * @param count       количество новых чеков
     * @param amount      общая сумма чеков
     */
    @PostMapping("/new-receipts")
    public ResponseEntity<ApiResponse<Object>> sendNewReceiptsNotification(
            @RequestParam String phoneNumber,
            @RequestParam int count,
            @RequestParam String amount) {

        log.info("Демо: отправка уведомления о новых чеках для {}", phoneNumber);

        return sendTypedNotification(
                phoneNumber,
                NotificationType.NEW_RECEIPTS_AVAILABLE,
                NotificationParams.builder()
                        .phoneNumber(phoneNumber)
                        .templateVariables(Map.of(
                                "count", String.valueOf(count),
                                "amount", amount
                        ))
                        .build()
        );
    }

    /**
     * Отправка ежемесячной статистики.
     * Демонстрирует уведомление MONTHLY_STATISTICS.
     *
     * @param phoneNumber номер телефона пользователя
     * @param month       название месяца
     * @param count       количество чеков за месяц
     * @param amount      общая сумма чеков
     * @param average     средний чек
     */
    @PostMapping("/monthly-stats")
    public ResponseEntity<ApiResponse<Object>> sendMonthlyStatsNotification(
            @RequestParam String phoneNumber,
            @RequestParam String month,
            @RequestParam int count,
            @RequestParam String amount,
            @RequestParam String average) {

        log.info("Демо: отправка месячной статистики для {}", phoneNumber);

        return sendTypedNotification(
                phoneNumber,
                NotificationType.MONTHLY_STATISTICS,
                NotificationParams.builder()
                        .phoneNumber(phoneNumber)
                        .templateVariables(Map.of(
                                "month", month,
                                "count", String.valueOf(count),
                                "amount", amount,
                                "average", average
                        ))
                        .build()
        );
    }

    /**
     * Отправка напоминания об истечении срока хранения чеков.
     * Демонстрирует уведомление RECEIPTS_EXPIRING_SOON.
     *
     * @param phoneNumber номер телефона пользователя
     */
    @PostMapping("/expiring-soon")
    public ResponseEntity<ApiResponse<Object>> sendExpiringSoonNotification(
            @RequestParam String phoneNumber) {

        log.info("Демо: отправка напоминания об истечении срока чеков для {}", phoneNumber);

        return sendTypedNotification(
                phoneNumber,
                NotificationType.RECEIPTS_EXPIRING_SOON,
                NotificationParams.builder()
                        .phoneNumber(phoneNumber)
                        .build()
        );
    }

    /**
     * Отправка уведомления о завершении подключения.
     * Демонстрирует уведомление BINDING_COMPLETED.
     *
     * @param phoneNumber номер телефона пользователя
     */
    @PostMapping("/binding-completed")
    public ResponseEntity<ApiResponse<Object>> sendBindingCompletedNotification(
            @RequestParam String phoneNumber) {

        log.info("Демо: отправка уведомления о завершении подключения для {}", phoneNumber);

        return sendTypedNotification(
                phoneNumber,
                NotificationType.BINDING_COMPLETED,
                NotificationParams.builder()
                        .phoneNumber(phoneNumber)
                        .build()
        );
    }

    /**
     * Получить список всех типов GENERAL уведомлений.
     * Показывает команде МЧО, что система уведомлений продумана.
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Object>> getNotificationTypes() {
        log.info("Демо: запрос списка типов уведомлений");

        List<Map<String, Object>> types = new ArrayList<>();

        for (NotificationType type : NotificationType.values()) {
            Map<String, Object> typeInfo = new LinkedHashMap<>();
            typeInfo.put("name", type.name());
            typeInfo.put("category", type.getCategory());
            typeInfo.put("titleTemplate", type.getTitleTemplate());
            typeInfo.put("messageTemplate", type.getMessageTemplate());
            typeInfo.put("shortMessageTemplate", type.getShortMessageTemplate());
            typeInfo.put("description", getTypeDescription(type));
            types.add(typeInfo);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTypes", types.size());
        data.put("category", "GENERAL");
        data.put("categoryExplanation", "Используем GENERAL вместо CASHBACK, т.к. сервис предназначен для работы с чеками, а не кешбэком");
        data.put("protocolReference", "Соответствует требованиям протокола МЧО (п. 3.4)");
        data.put("types", types);

        return ResponseEntity.ok(ApiResponse.success(
                "Список всех типов GENERAL уведомлений",
                data
        ));
    }

    /**
     * Общий метод отправки типизированного уведомления с обработкой ошибок.
     */
    private ResponseEntity<ApiResponse<Object>> sendTypedNotification(
            String phoneNumber,
            NotificationType type,
            NotificationParams params) {

        try {
            PostNotificationResponse response = mcoService.sendTypedNotification(
                    phoneNumber,
                    type,
                    params
            );

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("requestId", response.getRequestId());
            data.put("type", type.name());
            data.put("category", type.getCategory());
            data.put("phoneNumber", phoneNumber);
            data.put("handledAt", response.getHandledAt());
            data.put("sentAt", LocalDateTime.now());

            // Добавляем заполненные тексты для наглядности
            data.put("sentTitle", templateService.getTitle(type, params.getTemplateVariables()));
            data.put("sentMessage", templateService.getMessage(type, params.getTemplateVariables()));
            data.put("sentShortMessage", templateService.getShortMessage(type, params.getTemplateVariables()));

            return ResponseEntity.ok(ApiResponse.success(
                    "Уведомление " + type.name() + " успешно отправлено",
                    data
            ));

        } catch (BusinessMcoException e) {
            return handleBusinessException(e, phoneNumber, type);
        } catch (RetryableMcoException e) {
            log.warn("Повторяемая ошибка при отправке {} для {}: {}",
                    type.name(), phoneNumber, e.getErrorCode().getCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Сервис временно недоступен — повторите позже"
                    ));
        } catch (FatalMcoException e) {
            log.error("Фатальная ошибка при отправке {} для {}: {}",
                    type.name(), phoneNumber, e.getErrorCode().getCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Внутренняя ошибка сервера"
                    ));
        } catch (Exception e) {
            log.error("Необработанная ошибка при отправке {} для {}",
                    type.name(), phoneNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутренняя ошибка при отправке уведомления"));
        }
    }

    /**
     * Обработка бизнес-ошибок с понятными сообщениями.
     */
    private ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessMcoException e,
            String phoneNumber,
            NotificationType type) {

        if (McoErrorCode.USER_NOT_BOUND.equals(e.getErrorCode()) ||
                McoErrorCode.USER_IDENTIFIER_NOT_FOUND.equals(e.getErrorCode()) ||
                McoErrorCode.USER_IDENTIFIER_UNBOUND.equals(e.getErrorCode()) ||
                McoErrorCode.IDENTIFIER_UNBOUND.equals(e.getErrorCode())) {

            log.info("Пользователь {} не подключен, уведомление {} не отправлено",
                    phoneNumber, type.name());

            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            "Пользователь не подключен к сервису",
                            e.getErrorCode().getCode(),
                            "Для отправки уведомлений пользователь должен быть подключен через /api/mco/bind-user"
                    )
            );
        }

        if (McoErrorCode.NOTIFICATION_PERMISSION_DENIED.equals(e.getErrorCode())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Пользователь запретил получение уведомлений"
                    )
            );
        }

        if (McoErrorCode.DUPLICATE_NOTIFICATION.equals(e.getErrorCode()) ||
                McoErrorCode.NOTIFICATION_RATE_LIMIT_EXCEEDED.equals(e.getErrorCode())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    ApiResponse.error(
                            e.getErrorMessage(),
                            e.getErrorCode().getCode(),
                            "Превышен лимит уведомлений — попробуйте позже"
                    )
            );
        }

        log.info("Бизнес-ошибка при отправке {} для {}: {} - {}",
                type.name(), phoneNumber, e.getErrorCode().getCode(), e.getErrorMessage());

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        e.getErrorMessage(),
                        e.getErrorCode().getCode(),
                        "Ошибка при отправке уведомления"
                )
        );
    }

    /**
     * Описание типа уведомления для документации.
     */
    private String getTypeDescription(NotificationType type) {
        return switch (type) {
            case NEW_RECEIPTS_AVAILABLE ->
                    "Отправляется когда в системе появились новые чеки пользователя";
            case MONTHLY_STATISTICS ->
                    "Отправляется в начале месяца с итогами предыдущего периода";
            case RECEIPTS_EXPIRING_SOON ->
                    "Напоминание о том, что чеки хранятся 5 дней и скоро будут удалены";
            case BINDING_COMPLETED ->
                    "Отправляется после успешного одобрения заявки на подключение";
            case UNBINDING_COMPLETED ->
                    "Отправляется когда пользователь отключился от партнера";
            case BINDING_REMINDER ->
                    "Напоминание о необходимости подтвердить заявку в ЛК МЧО";
            case SERVICE_UPDATE ->
                    "Информирование о новых возможностях сервиса";
        };
    }
}
