package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.response.GetBindPartnerStatusResponse;
import org.example.mcoService.exception.RetryableMcoException;
import org.example.common.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BindApprovalPollingService {

    private final McoService mcoService;
    private final UserRepository userRepository;

    @Async
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

                    userRepository.findByPhoneNumber(phone).ifPresent(user -> {
                        user.setPartnerConnected(true);
                        userRepository.save(user);
                        log.info("Статус партнерского подключения обновлен для пользователя {}", phone);
                    });

                    return;

                } else if (isFinalStatus(result)) {
                    log.warn("Заявка {} завершена со статусом: {}", requestId, result);
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
                return;
            }
        }

        log.warn("Таймаут опроса для заявки {} (6 минут истекло)", requestId);
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