package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.notification.NotificationParams;
import org.example.mcoService.dto.response.PostNotificationResponse;
import org.example.mcoService.enums.NotificationType;
import org.example.mcoService.exception.McoException;
import org.springframework.stereotype.Service;

/**
 * Сервис для автоматической отправки уведомлений.
 * Используется для production-сценариев, когда уведомления
 * отправляются автоматически при определённых событиях.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoNotificationService {

    private final McoService mcoService;

    /**
     * Отправить уведомление о завершении подключения.
     * Вызывается автоматически из PollingService после REQUEST_APPROVED.
     *
     * @param phoneNumber номер телефона пользователя
     */
    public void sendBindingCompletedNotification(String phoneNumber) {
        log.info("Автоматическая отправка уведомления BINDING_COMPLETED для {}", phoneNumber);

        try {
            NotificationParams params = NotificationParams.builder()
                    .phoneNumber(phoneNumber)
                    .build();

            PostNotificationResponse response = mcoService.sendTypedNotification(
                    phoneNumber,
                    NotificationType.BINDING_COMPLETED,
                    params
            );

            log.info("Уведомление BINDING_COMPLETED успешно отправлено для {}, requestId: {}",
                    phoneNumber, response.getRequestId());

        } catch (McoException e) {
            // Логируем, но не прерываем основной процесс
            log.warn("Не удалось отправить уведомление BINDING_COMPLETED для {}: {} - {}",
                    phoneNumber, e.getErrorCode().getCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отправке уведомления BINDING_COMPLETED для {}",
                    phoneNumber, e);
        }
    }

    /**
     * Отправить уведомление об отключении от сервиса.
     * Вызывается автоматически при отключении пользователя от партнера.
     *
     * @param phoneNumber номер телефона пользователя
     */
    public void sendUnbindingCompletedNotification(String phoneNumber) {
        log.info("Автоматическая отправка уведомления UNBINDING_COMPLETED для {}", phoneNumber);

        try {
            NotificationParams params = NotificationParams.builder()
                    .phoneNumber(phoneNumber)
                    .build();

            PostNotificationResponse response = mcoService.sendTypedNotification(
                    phoneNumber,
                    NotificationType.UNBINDING_COMPLETED,
                    params
            );

            log.info("Уведомление UNBINDING_COMPLETED успешно отправлено для {}, requestId: {}",
                    phoneNumber, response.getRequestId());

        } catch (McoException e) {
            // Логируем, но не прерываем основной процесс
            log.warn("Не удалось отправить уведомление UNBINDING_COMPLETED для {}: {} - {}",
                    phoneNumber, e.getErrorCode().getCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отправке уведомления UNBINDING_COMPLETED для {}",
                    phoneNumber, e);
        }
    }

    /**
     * Отправить напоминание о необходимости подтвердить заявку.
     * Может вызываться по расписанию для пользователей с незавершённым подключением.
     *
     * @param phoneNumber номер телефона пользователя
     */
    public void sendBindingReminderNotification(String phoneNumber) {
        log.info("Автоматическая отправка уведомления BINDING_REMINDER для {}", phoneNumber);

        try {
            NotificationParams params = NotificationParams.builder()
                    .phoneNumber(phoneNumber)
                    .build();

            PostNotificationResponse response = mcoService.sendTypedNotification(
                    phoneNumber,
                    NotificationType.BINDING_REMINDER,
                    params
            );

            log.info("Уведомление BINDING_REMINDER успешно отправлено для {}, requestId: {}",
                    phoneNumber, response.getRequestId());

        } catch (McoException e) {
            log.warn("Не удалось отправить уведомление BINDING_REMINDER для {}: {} - {}",
                    phoneNumber, e.getErrorCode().getCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отправке уведомления BINDING_REMINDER для {}",
                    phoneNumber, e);
        }
    }

    /**
     * Отправить уведомление о новых чеках.
     * Вызывается после синхронизации чеков.
     *
     * @param phoneNumber номер телефона пользователя
     * @param count       количество новых чеков
     * @param totalAmount общая сумма чеков
     */
    public void sendNewReceiptsNotification(String phoneNumber, int count, String totalAmount) {
        log.info("Автоматическая отправка уведомления NEW_RECEIPTS_AVAILABLE для {}: {} чеков на {}₽",
                phoneNumber, count, totalAmount);

        try {
            NotificationParams params = NotificationParams.builder()
                    .phoneNumber(phoneNumber)
                    .templateVariables(java.util.Map.of(
                            "count", String.valueOf(count),
                            "amount", totalAmount
                    ))
                    .build();

            PostNotificationResponse response = mcoService.sendTypedNotification(
                    phoneNumber,
                    NotificationType.NEW_RECEIPTS_AVAILABLE,
                    params
            );

            log.info("Уведомление NEW_RECEIPTS_AVAILABLE успешно отправлено для {}, requestId: {}",
                    phoneNumber, response.getRequestId());

        } catch (McoException e) {
            log.warn("Не удалось отправить уведомление NEW_RECEIPTS_AVAILABLE для {}: {} - {}",
                    phoneNumber, e.getErrorCode().getCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отправке уведомления NEW_RECEIPTS_AVAILABLE для {}",
                    phoneNumber, e);
        }
    }

    /**
     * Отправить напоминание об истечении срока хранения чеков.
     * Вызывается по расписанию для пользователей с непросмотренными чеками.
     *
     * @param phoneNumber номер телефона пользователя
     */
    public void sendReceiptsExpiringSoonNotification(String phoneNumber) {
        log.info("Автоматическая отправка уведомления RECEIPTS_EXPIRING_SOON для {}", phoneNumber);

        try {
            NotificationParams params = NotificationParams.builder()
                    .phoneNumber(phoneNumber)
                    .build();

            PostNotificationResponse response = mcoService.sendTypedNotification(
                    phoneNumber,
                    NotificationType.RECEIPTS_EXPIRING_SOON,
                    params
            );

            log.info("Уведомление RECEIPTS_EXPIRING_SOON успешно отправлено для {}, requestId: {}",
                    phoneNumber, response.getRequestId());

        } catch (McoException e) {
            log.warn("Не удалось отправить уведомление RECEIPTS_EXPIRING_SOON для {}: {} - {}",
                    phoneNumber, e.getErrorCode().getCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отправке уведомления RECEIPTS_EXPIRING_SOON для {}",
                    phoneNumber, e);
        }
    }
}
