package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.response.GetBindPartnerStatusResponse;
import org.example.common.entity.UserBindingStatus;
import org.example.mcoService.exception.RetryableMcoException;
import org.example.common.repository.UserBindingStatusRepository;
import org.example.common.repository.UserRepository;
import org.example.mcoService.websocket.BindStatusWebSocketHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BindApprovalPollingService {

    private final McoService mcoService;
    private final UserRepository userRepository;
    private final UserBindingStatusRepository bindingStatusRepository;
    private final AutoNotificationService autoNotificationService;
    private final BindStatusWebSocketHandler webSocketHandler;
    private final ReceiptMarkerService receiptMarkerService;
    private final ReceiptService receiptService;

    @Async
    @Transactional
    public void startPollingAndUpdateOnApproval(String requestId, String phone) {
        log.info("Запуск фонового опроса статуса заявки {} для пользователя {}", requestId, phone);

        int maxAttempts = 12;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                attempt++;
                Thread.sleep(30_000);

                log.debug("Попытка {}/{} проверки статуса заявки {}", attempt, maxAttempts, requestId);

                GetBindPartnerStatusResponse.BindPartnerStatus status =
                        mcoService.checkBindRequestStatus(requestId);

                String result = status != null ? status.getResult() : null;

                if ("REQUEST_APPROVED".equals(result)) {
                    log.info("Заявка {} одобрена! Обновляем статус пользователя {}", requestId, phone);

                    // 1. Обновляем статус в таблице User
                    userRepository.findByPhoneNumberNormalized(phone).ifPresent(user -> {
                        userRepository.save(user);
                        log.info("Статус партнерского подключения обновлен для пользователя {}", phone);
                    });

                    // 2. Создаем/обновляем запись в UserBindingStatus
                    UserBindingStatus bindingStatus = bindingStatusRepository.createOrUpdateBindingStatus(
                            phone,
                            requestId,
                            UserBindingStatus.BindingStatus.APPROVED
                    );

                    log.info("Создана/обновлена запись в user_binding_status для пользователя {}, ID: {}",
                            phone, bindingStatus.getId());

                    // 3. Отправляем WebSocket уведомление о подключении
                    webSocketHandler.sendBindStatusNotification(requestId, "APPROVED", phone);

                    // 4. Мгновенная синхронизация чеков после подключения
                    syncReceiptsForUser(phone);

                    // 5. Отправляем уведомление о завершении подключения (push)
                    autoNotificationService.sendBindingCompletedNotification(phone);

                    return;

                } else if (isFinalStatus(result)) {
                    log.warn("Заявка {} завершена со статусом: {}", requestId, result);

                    // Даже если статус не APPROVED, сохраняем информацию
                    UserBindingStatus.BindingStatus bindingStatus = mapResultToBindingStatus(result);
                    bindingStatusRepository.createOrUpdateBindingStatus(
                            phone,
                            requestId,
                            bindingStatus
                    );

                    // Отправляем WebSocket уведомление о статусе
                    webSocketHandler.sendBindStatusNotification(requestId, result, phone);

                    return;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Опрос прерван для заявки {}", requestId);
                return;

            } catch (RetryableMcoException e) {
                log.warn("Retryable ошибка при проверке статуса {}: {}. Продолжаем опрос", requestId, e.getMessage());

            } catch (Exception e) {
                log.error("Критическая ошибка при проверке статуса заявки {}: {}", requestId, e.getMessage(), e);
                webSocketHandler.sendErrorNotification(requestId, e.getMessage());
                return;
            }
        }

        log.warn("Таймаут опроса для заявки {} (6 минут истекло)", requestId);

        bindingStatusRepository.createOrUpdateBindingStatus(
                phone,
                requestId,
                UserBindingStatus.BindingStatus.EXPIRED
        );
        
        webSocketHandler.sendBindStatusNotification(requestId, "EXPIRED", phone);
    }

    /**
     * Мгновенная синхронизация чеков для пользователя после подключения
     */
    private void syncReceiptsForUser(String phone) {
        try {
            log.info("Мгновенная синхронизация чеков для пользователя {}", phone);
            
            // Получаем чеки с самого начала (S_FROM_BEGINNING)
            String marker = "S_FROM_BEGINNING";
            
            var response = mcoService.getReceiptsByMarker(marker);
            
            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                var userReceipts = response.getReceipts().stream()
                        .filter(r -> phone.equals(r.getUserIdentifier()) || phone.equals(r.getPhone()))
                        .toList();
                
                if (!userReceipts.isEmpty()) {
                    var result = receiptService.saveReceipts(userReceipts);
                    log.info("Синхронизировано {} новых чеков для {} на сумму {}",
                            result.count(), phone, result.getTotalSumFormatted());
                    
                    // Отправляем WebSocket уведомление о новых чеках
                    if (result.hasNewReceipts()) {
                        webSocketHandler.sendNewReceiptsNotification(phone, result.count(), result.getTotalSumFormatted());
                    }
                }
            }
            
            // Сохраняем последний маркер для последующей периодической синхронизации
            if (response.getNextMarker() != null) {
                receiptMarkerService.saveMarker(phone, response.getNextMarker());
            }
            
        } catch (Exception e) {
            log.error("Ошибка мгновенной синхронизации чеков для {}: {}", phone, e.getMessage());
        }
    }

    private UserBindingStatus.BindingStatus mapResultToBindingStatus(String result) {
        if (result == null) return UserBindingStatus.BindingStatus.PENDING;

        return switch (result) {
            case "REQUEST_APPROVED" -> UserBindingStatus.BindingStatus.APPROVED;
            case "REQUEST_DECLINED" -> UserBindingStatus.BindingStatus.DECLINED;
            case "REQUEST_CANCELLED_AS_DUPLICATE" -> UserBindingStatus.BindingStatus.CANCELLED;
            case "REQUEST_EXPIRED" -> UserBindingStatus.BindingStatus.EXPIRED;
            case "REQUEST_IN_PROGRESS" -> UserBindingStatus.BindingStatus.IN_PROGRESS;
            default -> UserBindingStatus.BindingStatus.PENDING;
        };
    }

    private boolean isFinalStatus(String result) {
        if (result == null) return false;
        return switch (result) {
            case "REQUEST_APPROVED",
                 "REQUEST_DECLINED",
                 "REQUEST_CANCELLED_AS_DUPLICATE",
                 "REQUEST_EXPIRED" -> true;
            default -> false;
        };
    }
}