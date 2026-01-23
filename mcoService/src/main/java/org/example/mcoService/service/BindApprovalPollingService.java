package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.response.GetBindPartnerStatusResponse;
import org.example.mcoService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.Duration;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class BindApprovalPollingService {

    private final McoService mcoService;
    private final UserRepository userRepository;

    // Вот здесь он уже пришёл через Spring (по имени бина)
    @Qualifier("mcoPollingExecutor")
    private final TaskExecutor taskExecutor;

    public void startPollingAndUpdateOnApproval(String requestId, String phone) {

        // ← Самое важное место: передаём taskExecutor вторым аргументом
        CompletableFuture.runAsync(() -> {

                    Instant now = Instant.now();
                    Instant deadline = now.plus(Duration.ofMinutes(6));

                    while (Instant.now().isBefore(deadline)) {

                        try {
                            GetBindPartnerStatusResponse.BindPartnerStatus status =
                                    mcoService.checkBindRequestStatus(requestId);

                            String result = status != null ? status.getResult() : null;

                            if ("REQUEST_APPROVED".equals(result)) {
                                log.info("Заявка одобрена → обновляем пользователя по телефону {}", phone);

                                userRepository.findByPhone(phone).ifPresentOrElse(   // ← пример улучшения
                                        user -> {
                                            user.setPartnerConnected(true);
                                            userRepository.save(user);
                                            log.info("Пользователь {} → partnerConnected = true (requestId: {})", phone, requestId);
                                        },
                                        () -> log.warn("Пользователь с телефоном {} не найден в БД", phone)
                                );

                                return;
                            }

                            if (isFinalStatus(result)) {
                                log.info("Заявка в финальном состоянии {}, больше не опрашиваем: {}", result, requestId);
                                return;
                            }

                        } catch (Exception e) {
                            log.warn("Ошибка проверки статуса заявки {}: {}", requestId, e.getMessage(), e);
                        }

                        try {
                            Thread.sleep(10_000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Polling прерван для заявки {}", requestId);
                            return;
                        }
                    }

                    log.info("Таймаут 6 минут истёк, статус не стал APPROVED для заявки {}", requestId);

                }, taskExecutor)   // ← вот здесь используется!

                .exceptionally(ex -> {
                    log.error("Polling задача упала для requestId {}: {}", requestId, ex.getMessage(), ex);
                    return null;
                });
    }

    private boolean isFinalStatus(String result) {
        if (result == null) return false;
        return switch (result) {
            case "REQUEST_APPROVED", "REQUEST_DECLINED",
                 "REQUEST_CANCELLED_AS_DUPLICATE", "REQUEST_EXPIRED" -> true;
            default -> false;
        };
    }
}