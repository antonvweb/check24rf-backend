package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.example.mcoService.dto.response.SendMessageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class McoService {

    private final McoApiClient apiClient;
    private final McoProperties properties;

    public String initializePartner(String logoPath) {
        try {
            byte[] logoBytes = Files.readAllBytes(Path.of(logoPath));
            if (logoBytes.length > 100 * 1024) {
                throw new IllegalArgumentException("Размер логотипа превышает 100 КБ");
            }

            String mimeType = Files.probeContentType(Path.of(logoPath));
            if (!"image/jpeg".equals(mimeType)) {
                throw new IllegalArgumentException("Логотип должен быть в формате JPEG");
            }

            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);

            PostPlatformRegistrationResponse response = apiClient.registerPartnerSync(
                    properties.getPartner().getName(),
                    "Описание",
                    "https://xn--24-mlcu7d.xn--p1ai/",
                    base64Logo,
                    properties.getPartner().getInn(),
                    "79991234567"
            );

            log.info("Партнер РЕАЛЬНО зарегистрирован, ID: {}", response.getId());
            return response.getId(); // Возвращаем РЕАЛЬНЫЙ ID

        } catch (IOException e) {
            log.error("Ошибка чтения логотипа", e);
            throw new RuntimeException("Не удалось загрузить логотип", e);
        }
    }

    public String connectUser(String phone) {
        String requestId = UUID.randomUUID().toString().toUpperCase();

        PostBindPartnerResponse response = apiClient.bindUserSync(phone, requestId);

        log.info("Заявка на подключение пользователя {} отправлена, MessageId: {}, RequestId: {}",
                phone, response.getMessageId(), requestId);

        return response.getMessageId();
    }

    public void syncReceipts() {
        log.info("Начало синхронизации чеков");
        apiClient.getAllReceipts();
        log.info("Синхронизация чеков завершена");
    }
}