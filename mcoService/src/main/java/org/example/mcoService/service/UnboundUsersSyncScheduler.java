package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.repository.UserRepository;
import org.example.mcoService.dto.response.GetUnboundPartnerResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnboundUsersSyncScheduler {

    private final UserRepository userRepository;
    private final McoService mcoService;
    private final UnboundMarkerService unboundMarkerService;

    @Scheduled(fixedDelay = 300_000) // 5 минут, как и для чеков
    public void syncUnboundUsers() {
        log.info("Запуск периодической проверки отключившихся пользователей");

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
            int updatedCount = updateUnboundUsers(response.getUnbounds());
            log.info("Обновлено {} пользователей, отключившихся от партнера", updatedCount);
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
            // Используем рекурсивный вызов для обработки всех порций
            processNextUnboundBatch(response.getNextMarker());
        }
    }

    private void processNextUnboundBatch(String marker) {
        try {
            log.debug("Проверка следующей порции отключившихся пользователей с маркером: {}", marker);

            GetUnboundPartnerResponse response = mcoService.getUnboundPartners(marker);

            if (response.getUnbounds() != null && !response.getUnbounds().isEmpty()) {
                int updatedCount = updateUnboundUsers(response.getUnbounds());
                log.debug("Обновлено еще {} отключившихся пользователей", updatedCount);
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
    protected int updateUnboundUsers(List<GetUnboundPartnerResponse.UnboundUser> unboundUsers) {
        int updatedCount = 0;

        for (GetUnboundPartnerResponse.UnboundUser unboundUser : unboundUsers) {
            String phone = unboundUser.getUserIdentifier();
            System.out.println(phone);

            if (phone != null && !phone.isEmpty()) {
                try {
                    userRepository.findByPhoneNumber(phone).ifPresent(user -> {
                        if (user.isPartnerConnected()) {
                            user.setPartnerConnected(false);
                            userRepository.save(user);
                            log.debug("Пользователь {} отмечен как отключенный от партнера", phone);
                        }
                    });
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Ошибка при обновлении статуса пользователя {}: {}", phone, e.getMessage());
                }
            }
        }

        return updatedCount;
    }
}