package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class McoService {

    private final McoApiClient apiClient;
    private final McoProperties properties;

    /**
     * Инициализация партнера в системе
     */
    public String initializePartner(String logoPath) {
        try {
            byte[] logoBytes = Files.readAllBytes(Path.of(logoPath));

            PostPlatformRegistrationResponse response = apiClient.registerPartner(
                    properties.getPartner().getName(),
                    "Описание вашего сервиса кешбэка",
                    "https://xn--24-mlcu7d.xn--p1ai/",
                    logoBytes,
                    properties.getPartner().getInn(),
                    "79991234567"
            );

            log.info("Партнер зарегистрирован с ID: {}", response.getId());
            return response.getId();

        } catch (IOException e) {
            log.error("Ошибка чтения логотипа", e);
            throw new RuntimeException("Не удалось загрузить логотип", e);
        }
    }

    /**
     * Подключение пользователя
     */
    public void connectUser(String phone) {
        // Валидация формата телефона
        if (!phone.matches("^7\\d{10}$")) {
            throw new IllegalArgumentException(
                    "Неверный формат телефона. Ожидается: 79998887766"
            );
        }

        apiClient.bindUser(phone);
        log.info("Заявка на подключение пользователя {} отправлена", phone);
    }

    /**
     * Синхронизация чеков
     */
    public void syncReceipts() {
        log.info("Начало синхронизации чеков");
        apiClient.getAllReceipts();
        log.info("Синхронизация чеков завершена");
    }
}