package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.request.GetReceiptsTapeRequest;
import org.example.mcoService.dto.request.PostBindPartnerRequest;
import org.example.mcoService.dto.request.PostPlatformRegistrationRequest;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class McoApiClient {

    @Autowired
    private McoSoapClient soapClient;

    /**
     * Регистрация партнера в системе МЧО
     */
    public PostPlatformRegistrationResponse registerPartner(
            String name,
            String description,
            String transitionLink,
            byte[] logoImage,
            String inn,
            String phone) {

        log.info("Регистрация партнера: {}", name);

        PostPlatformRegistrationRequest request = PostPlatformRegistrationRequest.builder()
                .name(name)
                .type("PARTNER")
                .description(description)
                .transitionLink(transitionLink)
                .image(logoImage != null ? logoImage : new byte[0]) // Защита от null
                .inns(Collections.singletonList(inn))
                .phone(phone)
                .hidden(false)
                .build();


        return soapClient.sendSoapRequest(
                request,
                PostPlatformRegistrationResponse.class,
                "urn://x-artefacts-gnivc-ru/ais3/smz/SmzIntegrationService/v0.1/PostPlatformRegistration"
        );
    }

    /**
     * Подключение покупателя к партнеру
     */
    public PostBindPartnerResponse bindUser(String phoneNumber) {  // Измените void на возврат response
        log.info("Подключение пользователя: {}", phoneNumber);

        String requestId = UUID.randomUUID().toString();

        PostBindPartnerRequest request = PostBindPartnerRequest.builder()
                .requestId(requestId)
                .userIdentifier(phoneNumber)
                .permissionGroups(Collections.singletonList("DEFAULT"))
                .expiredAt(LocalDateTime.now().plusDays(7))
                .isUnverifiedIdentifier(false)
                .requireNoActiveRequests(false)
                .build();

        PostBindPartnerResponse response = soapClient.sendSoapRequest(
                request,
                PostBindPartnerResponse.class,  // Теперь типизировано
                "PostBindPartnerRequest"
        );

        log.info("Заявка отправлена, MessageId: {}", response.getMessageId());
        return response;  // Верните для дальнейшего опроса
    }

    /**
     * Получение ленты чеков
     */
    public GetReceiptsTapeResponse getReceipts(String marker) {
        log.info("Получение ленты чеков с маркером: {}", marker);

        GetReceiptsTapeRequest request = GetReceiptsTapeRequest.builder()
                .marker(marker != null ? marker : "S_FROM_END")
                .build();

        return soapClient.sendSoapRequest(
                request,
                GetReceiptsTapeResponse.class,
                "GetReceiptsTapeRequest"
        );
    }

    /**
     * Получение всех чеков с пагинацией
     */
    public void getAllReceipts() {
        String marker = "S_FROM_END";
        boolean hasMore = true;

        while (hasMore) {
            GetReceiptsTapeResponse response = getReceipts(marker);

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                log.info("Получено чеков: {}", response.getReceipts().size());

                // Обработка чеков
                response.getReceipts().forEach(receipt -> {
                    log.info("Чек от: {}, источник: {}",
                            receipt.getUserIdentifier(),
                            receipt.getSourceCode());
                });
            }

            marker = response.getNextMarker();

            // Проверяем, есть ли еще данные
            // Если TotalExpectedRemainingPolls близко к 1, значит дошли до конца
            hasMore = response.getTotalExpectedRemainingPolls() != null &&
                    response.getTotalExpectedRemainingPolls() > 1;
        }
    }
}