package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.User;
import org.example.common.repository.UserRepository;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptSyncScheduler {

    private final UserRepository userRepository;
    private final McoService mcoService;
    private final ReceiptService receiptService;
    private final ReceiptMarkerService markerService;

    @Scheduled(fixedDelay = 300_000)
    public void syncReceiptsForAllConnectedUsers() {
        log.info("=== ?????? ????????????? ????????????? ????? ===");

        List<String> connectedPhones = userRepository.findAllConnectedToPartner().stream()
                .map(User::getPhoneNumber)
                .toList();

        log.info("??????? {} ???????????? ?????????????", connectedPhones.size());

        for (String phone : connectedPhones) {
            try {
                syncReceiptsForUser(phone);
            } catch (Exception e) {
                log.error("?????? ????????????? ??? {}: {}", phone, e.getMessage());
            }
        }

        log.info("=== ????????????? ????????? ===");
    }

    private void syncReceiptsForUser(String phone) {
        String marker = markerService.getMarker(phone);

        log.debug("????????????? ????? ??? {} ? ???????? {}", phone, marker);

        GetReceiptsTapeResponse response = mcoService.getReceiptsByMarker(marker);

        if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
            List<GetReceiptsTapeResponse.Receipt> userReceipts = response.getReceipts().stream()
                    .filter(r -> phone.equals(r.getUserIdentifier()) || phone.equals(r.getPhone()))
                    .toList();

            int saved = receiptService.saveReceipts(userReceipts);
            log.info("???????????????? {} ????? ????? ??? {}", saved, phone);
        }

        if (response.getNextMarker() != null) {
            markerService.saveMarker(phone, response.getNextMarker());
        }
    }
}