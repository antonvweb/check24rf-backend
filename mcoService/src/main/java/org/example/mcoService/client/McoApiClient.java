package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.request.GetReceiptsTapeRequest;
import org.example.mcoService.dto.request.PostBindPartnerRequest;
import org.example.mcoService.dto.request.PostPlatformRegistrationRequest;
import org.example.mcoService.dto.request.SendMessageRequest;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.SendMessageResponse;
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

    public SendMessageResponse registerPartner(
            String name,
            String description,
            String transitionLink,
            String base64Logo,
            String inn,
            String phone) {

        log.info("Регистрация партнера: {}", name);

        PostPlatformRegistrationRequest innerRequest = PostPlatformRegistrationRequest.builder()
                .name(name)
                .type("PARTNER")
                .description(description)
                .transitionLink(transitionLink)
                .text(description)
                .image(base64Logo != null ? base64Logo : "")
                .imageFullscreen("")
                .inn(inn)
                .phone(phone)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse response = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Партнер зарегистрирован, MessageId: {}", response.getMessageId());
        return response;
    }

    public PostBindPartnerResponse bindUser(String phoneNumber) {
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
                PostBindPartnerResponse.class,
                "PostBindPartnerRequest"
        );

        log.info("Заявка отправлена, MessageId: {}", response.getMessageId());
        return response;
    }

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

    public void getAllReceipts() {
        String marker = "S_FROM_END";
        boolean hasMore = true;

        while (hasMore) {
            GetReceiptsTapeResponse response = getReceipts(marker);

            if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                log.info("Получено чеков: {}", response.getReceipts().size());

                response.getReceipts().forEach(receipt -> {
                    log.info("Чек от: {}, источник: {}",
                            receipt.getUserIdentifier(),
                            receipt.getSourceCode());
                });
            }

            marker = response.getNextMarker();
            hasMore = response.getTotalExpectedRemainingPolls() != null &&
                    response.getTotalExpectedRemainingPolls() > 1;
        }
    }
}