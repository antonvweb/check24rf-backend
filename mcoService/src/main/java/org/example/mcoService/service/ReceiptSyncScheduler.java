package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.UserBindingStatus;
import org.example.common.repository.UserBindingStatusRepository;
import org.example.mcoService.dto.api.SaveReceiptsResult;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptSyncScheduler {

    private final McoService mcoService;
    private final ReceiptService receiptService;
    private final ReceiptMarkerService markerService;
    private final UserBindingStatusRepository bindingStatusRepository;
    private final AutoNotificationService autoNotificationService;

    @Scheduled(fixedDelay = 300_000)
    public void syncReceiptsForAllConnectedUsers() {
        log.info("Запуск периодической синхронизации чеков");

        List<String> connectedPhones = bindingStatusRepository.findAll().stream()
                .map(UserBindingStatus::getPhoneNumber)
                .toList();

        log.info("Найдено {} подключенных пользователей", connectedPhones.size());

        for (String phone : connectedPhones) {
            try {
                syncReceiptsForUser(phone);
            } catch (Exception e) {
                log.error("Ошибка синхронизации для {}: {}", phone, e.getMessage());
            }
        }
        log.info("Синхронизация завершена");
    }

    private void syncReceiptsForUser(String phone) {
        String marker = markerService.getMarker(phone);

        log.debug("Синхронизация чеков для {} с маркером {}", phone, marker);

        GetReceiptsTapeResponse response = mcoService.getReceiptsByMarker(marker);

        if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
            List<GetReceiptsTapeResponse.Receipt> userReceipts = response.getReceipts().stream()
                    .filter(r -> phone.equals(r.getUserIdentifier()) || phone.equals(r.getPhone()))
                    .toList();

            SaveReceiptsResult result = receiptService.saveReceipts(userReceipts);
            log.info("Синхронизировано {} новых чеков для {} на сумму {}",
                    result.count(), phone, result.getTotalSumFormatted());

            // Отправляем уведомление о новых чеках
            if (result.hasNewReceipts()) {
                autoNotificationService.sendNewReceiptsNotification(
                        phone,
                        result.count(),
                        result.getTotalSumFormatted()
                );
            }
        }

        if (response.getNextMarker() != null) {
            markerService.saveMarker(phone, response.getNextMarker());
        }
    }
}