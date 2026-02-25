package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.repository.UserRepository;
import org.example.mcoService.dto.response.GetUnboundPartnerResponse;
import org.example.common.repository.UserBindingStatusRepository;
import org.example.mcoService.websocket.BindStatusWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnboundUsersSyncScheduler {

    private final UserRepository userRepository;
    private final UserBindingStatusRepository bindingStatusRepository;
    private final McoService mcoService;
    private final UnboundMarkerService unboundMarkerService;
    private final AutoNotificationService autoNotificationService;
    private final BindStatusWebSocketHandler webSocketHandler;

    // ОТКЛЮЧЕНО: Проверка отключившихся пользователей теперь работает через WebSocket
    // Метод вызывается вручную при необходимости или через тестовый endpoint
    // @Scheduled(fixedDelay = 300_000) // 5 минут
    public void syncUnboundUsers() {
        log.info("Запуск периодической проверки отключившихся пользователей (ОТКЛЮЧЕНО - используется WebSocket)");

        try {
            processUnboundUsersBatch();
        } catch (Exception e) {
            log.error("Ошибка при проверке отключившихся пользователей: {}", e.getMessage(), e);
        }

        log.info("Проверка отключившихся пользователей завершена");
    }

    private void processUnboundUsersBatch() {
        String marker = unboundMarkerService.getMarker();
        log.debug("Проверка отключившихся пользователей с маркером: {}", marker);

        GetUnboundPartnerResponse response = mcoService.getUnboundPartners(marker);

        if (response.getUnbounds() != null && !response.getUnbounds().isEmpty()) {
            int deletedCount = deleteUnboundUsers(response.getUnbounds());
            log.info("Обработано {} пользователей, отключившихся от партнера", deletedCount);
        } else {
            log.debug("Нет новых отключившихся пользователей");
        }

        // Сохраняем новый маркер для следующей проверки
        if (response.getNextMarker() != null) {
            unboundMarkerService.saveMarker(response.getNextMarker());
        }

        // Если есть еще данные (hasMore = true), обрабатываем следующую порцию рекурсивно
        if (Boolean.TRUE.equals(response.getHasMore()) && response.getNextMarker() != null) {
            log.debug("Есть еще данные, обрабатываем следующую порцию");
            processNextUnboundBatch(response.getNextMarker());
        }
    }

    private void processNextUnboundBatch(String marker) {
        try {
            log.debug("Проверка следующей порции отключившихся пользователей с маркером: {}", marker);

            GetUnboundPartnerResponse response = mcoService.getUnboundPartners(marker);

            if (response.getUnbounds() != null && !response.getUnbounds().isEmpty()) {
                int deletedCount = deleteUnboundUsers(response.getUnbounds());
                log.debug("Обработано еще {} отключившихся пользователей", deletedCount);
            }

            // Сохраняем новый маркер
            if (response.getNextMarker() != null) {
                unboundMarkerService.saveMarker(response.getNextMarker());
            }

            // Рекурсивно обрабатываем следующие порции, если они есть
            if (Boolean.TRUE.equals(response.getHasMore()) && response.getNextMarker() != null) {
                processNextUnboundBatch(response.getNextMarker());
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке следующей порции отключившихся пользователей: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected int deleteUnboundUsers(List<GetUnboundPartnerResponse.UnboundUser> unboundUsers) {
        int processedCount = 0;

        for (GetUnboundPartnerResponse.UnboundUser unboundUser : unboundUsers) {
            String phone = unboundUser.getUserIdentifier();

            if (phone != null && !phone.isEmpty()) {
                try {
                    // ВРЕМЕННО УДАЛЕНО: Удаление чеков пользователя
                    // userRepository.findByPhoneNumberNormalized(phone).ifPresent(user -> {
                    //     UUID userId = user.getId();
                    //     long deletedReceipts = deleteUserReceiptsInBatches(userId, phone);
                    //     log.info("Удалено {} чеков пользователя {}", deletedReceipts, phone);
                    // });

                    // 3. Удаляем запись из user_binding_status
                    bindingStatusRepository.findByPhoneNumberNormalized(phone).ifPresent(bindingStatus -> {
                        bindingStatusRepository.delete(bindingStatus);
                        log.info("Статус подключения удален для пользователя {}", phone);

                        // 4. Отправляем WebSocket уведомление об отключении
                        webSocketHandler.sendUnbindNotification(phone, "Пользователь отключился");

                        // 5. Отправляем push-уведомление об отключении
                        autoNotificationService.sendUnbindingCompletedNotification(phone);
                    });

                    processedCount++;

                } catch (Exception e) {
                    log.error("Ошибка при удалении пользователя {}: {}", phone, e.getMessage(), e);
                }
            }
        }

        return processedCount;
    }

    // ВРЕМЕННО УДАЛЕНО: Метод пакетного удаления чеков по userId
    // @Transactional
    // protected long deleteUserReceiptsInBatches(UUID userId, String phone) {
    //     try {
    //         // Сначала получаем общее количество чеков
    //         long totalReceipts = receiptRepository.countByUserId(userId);
    //         if (totalReceipts == 0) {
    //             log.debug("У пользователя {} нет чеков для удаления", phone);
    //             return 0;
    //         }
    //
    //         log.info("Начинаем пакетное удаление {} чеков пользователя {}", totalReceipts, phone);
    //
    //         long totalDeleted = 0;
    //         int batchDeleted;
    //         int batchNumber = 0;
    //
    //         do {
    //             batchNumber++;
    //
    //             // Удаляем пакет чеков
    //             batchDeleted = receiptRepository.deleteBatchByUserId(userId, BATCH_SIZE);
    //             totalDeleted += batchDeleted;
    //
    //             log.debug("Пакет {}: удалено {} чеков, всего удалено: {}/{}",
    //                     batchNumber, batchDeleted, totalDeleted, totalReceipts);
    //
    //             // Делаем паузу между пакетами, если удалили полный пакет
    //             if (batchDeleted == BATCH_SIZE) {
    //                 try {
    //                     Thread.sleep(BATCH_PAUSE_MS);
    //                 } catch (InterruptedException e) {
    //                     Thread.currentThread().interrupt();
    //                     log.warn("Пакетное удаление прервано для пользователя {}", phone);
    //                     break;
    //                 }
    //             }
    //
    //         } while (batchDeleted == BATCH_SIZE);
    //
    //         log.info("Завершено пакетное удаление: всего удалено {} чеков пользователя {}", totalDeleted, phone);
    //         return totalDeleted;
    //
    //     } catch (Exception e) {
    //         log.error("Ошибка при пакетном удалении чеков для пользователя {}: {}", phone, e.getMessage(), e);
    //         return 0;
    //     }
    // }

    // ВРЕМЕННО УДАЛЕНО: Методы пакетного удаления чеков
    // @Transactional
    // protected long deleteUserReceiptsInBatches(UUID userId, String phone) {
    //     try {
    //         // Сначала получаем общее количество чеков
    //         long totalReceipts = receiptRepository.countByUserId(userId);
    //         if (totalReceipts == 0) {
    //             log.debug("У пользователя {} нет чеков для удаления", phone);
    //             return 0;
    //         }
    //
    //         log.info("Начинаем пакетное удаление {} чеков пользователя {}", totalReceipts, phone);
    //
    //         long totalDeleted = 0;
    //         int batchDeleted;
    //         int batchNumber = 0;
    //
    //         do {
    //             batchNumber++;
    //
    //             // Удаляем пакет чеков
    //             batchDeleted = receiptRepository.deleteBatchByUserId(userId, BATCH_SIZE);
    //             totalDeleted += batchDeleted;
    //
    //             log.debug("Пакет {}: удалено {} чеков, всего удалено: {}/{}",
    //                     batchNumber, batchDeleted, totalDeleted, totalReceipts);
    //
    //             // Делаем паузу между пакетами, если удалили полный пакет
    //             if (batchDeleted == BATCH_SIZE) {
    //                 try {
    //                     Thread.sleep(BATCH_PAUSE_MS);
    //                 } catch (InterruptedException e) {
    //                     Thread.currentThread().interrupt();
    //                     log.warn("Пакетное удаление прервано для пользователя {}", phone);
    //                     break;
    //                 }
    //             }
    //
    //         } while (batchDeleted == BATCH_SIZE);
    //
    //         log.info("Завершено пакетное удаление: всего удалено {} чеков пользователя {}", totalDeleted, phone);
    //         return totalDeleted;
    //
    //     } catch (Exception e) {
    //         log.error("Ошибка при пакетном удалении чеков для пользователя {}: {}", phone, e.getMessage(), e);
    //         return 0;
    //     }
    // }
    //
    // // Альтернативный метод: удаление по userIdentifier (если userId не сохраняется в чеках)
    // @Transactional
    // protected long deleteUserReceiptsByIdentifierInBatches(String userIdentifier) {
    //     try {
    //         // Сначала получаем общее количество чеков
    //         long totalReceipts = receiptRepository.countByUserIdentifier(userIdentifier);
    //         if (totalReceipts == 0) {
    //             log.debug("У пользователя {} нет чеков для удаления", userIdentifier);
    //             return 0;
    //         }
    //
    //         log.info("Начинаем пакетное удаление {} чеков пользователя {}", totalReceipts, userIdentifier);
    //
    //         long totalDeleted = 0;
    //         int batchDeleted;
    //         int batchNumber = 0;
    //
    //         do {
    //             batchNumber++;
    //
    //             // Удаляем пакет чеков
    //             batchDeleted = receiptRepository.deleteBatchByUserIdentifier(userIdentifier, BATCH_SIZE);
    //             totalDeleted += batchDeleted;
    //
    //             log.debug("Пакет {}: удалено {} чеков, всего удалено: {}/{}",
    //                     batchNumber, batchDeleted, totalDeleted, totalReceipts);
    //
    //             // Делаем паузу между пакетами
    //             if (batchDeleted == BATCH_SIZE) {
    //                 try {
    //                     Thread.sleep(BATCH_PAUSE_MS);
    //                 } catch (InterruptedException e) {
    //                     Thread.currentThread().interrupt();
    //                     log.warn("Пакетное удаление прервано для пользователя {}", userIdentifier);
    //                     break;
    //                 }
    //             }
    //
    //         } while (batchDeleted == BATCH_SIZE);
    //
    //         log.info("Завершено пакетное удаление: всего удалено {} чеков пользователя {}", totalDeleted, userIdentifier);
    //         return totalDeleted;
    //
    //     } catch (Exception e) {
    //         log.error("Ошибка при пакетном удалении чеков для пользователя {}: {}", userIdentifier, e.getMessage(), e);
    //         return 0;
    //     }
    // }
}