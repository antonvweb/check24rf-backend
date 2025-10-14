package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

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
                    logoBytes, // ✅ вот здесь передаём Base64-строку в байтах
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
    public PostBindPartnerResponse connectUser(String phone) {  // Возвращайте response
        // Валидация...
        PostBindPartnerResponse response = apiClient.bindUser(phone);
        log.info("Заявка на подключение пользователя {} отправлена, MessageId: {}", phone, response.getMessageId());
        return response;  // Сохраните MessageId для опроса статуса позже
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