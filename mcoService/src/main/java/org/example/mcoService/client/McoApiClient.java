package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.request.GetReceiptsTapeRequest;
import org.example.mcoService.dto.request.PostBindPartnerRequest;
import org.example.mcoService.dto.request.PostPlatformRegistrationRequest;
import org.example.mcoService.dto.request.SendMessageRequest;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.dto.response.PostBindPartnerResponse;
import org.example.mcoService.dto.response.PostPlatformRegistrationResponse;
import org.example.mcoService.dto.response.SendMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;

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

        log.info("=== НАЧАЛО РЕГИСТРАЦИИ ===");
        log.info("name: [{}]", name);
        log.info("name length: {}", name != null ? name.length() : "null");
        log.info("name bytes: {}", name != null ? java.util.Arrays.toString(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)) : "null");
        log.info("type: PARTNER");
        log.info("inn: {}", inn);
        log.info("phone: {}", phone);

        PostPlatformRegistrationRequest innerRequest = PostPlatformRegistrationRequest.builder()
                .name(name)
                .type("PARTNER")
                .description(description)
                .transitionLink(transitionLink)
                .text(description)
                .image(base64Logo != null ? base64Logo : "")
                .imageFullScreen("")
                .inn(inn)
                .phone(phone)
                .build();

        log.info("=== СОЗДАН ОБЪЕКТ ===");
        log.info("innerRequest.name: [{}]", innerRequest.getName());

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

    public PostPlatformRegistrationResponse registerPartnerSync(
            String name,
            String description,
            String transitionLink,
            String base64Logo,
            String inn,
            String phone) {

        log.info("Регистрация партнера: {}", name);

        // Отправляем запрос
        SendMessageResponse messageResponse = registerPartner(
                name, description, transitionLink, base64Logo, inn, phone
        );

        log.info("Получен MessageId: {}, ожидаем результата...", messageResponse.getMessageId());

        try {
            // Опрашиваем результат
            PostPlatformRegistrationResponse response =
                    soapClient.getAsyncResult(
                            messageResponse.getMessageId(),
                            PostPlatformRegistrationResponse.class
                    );

            log.info("Партнер зарегистрирован, ID: {}", response.getId());
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }

    public SendMessageResponse bindUser(String phoneNumber, String requestId) {
        log.info("Подключение пользователя: {}", phoneNumber);

        PostBindPartnerRequest innerRequest = PostBindPartnerRequest.builder()
                .requestId(requestId)
                .userIdentifier(phoneNumber)
                .permissionGroups(Collections.singletonList("DEFAULT"))
                .expiredAt(LocalDateTime.now().plusDays(7))
                .isUnverifiedIdentifier(false)
                .requireNoActiveRequests(false)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        return soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );
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

    public PostBindPartnerResponse bindUserSync(String phoneNumber, String requestId) {
        log.info("Синхронное подключение пользователя: {}", phoneNumber);

        // Отправляем запрос
        SendMessageResponse messageResponse = bindUser(phoneNumber, requestId);

        log.info("Получен MessageId: {}, ожидаем результата...", messageResponse.getMessageId());

        try {
            // Опрашиваем результат (используем существующий getAsyncResult из McoSoapClient)
            PostBindPartnerResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    PostBindPartnerResponse.class  // Укажите правильный класс ответа из DTO
            );

            log.info("Заявка обработана успешно: {}", response);  // Здесь логируйте детали ответа
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        } catch (RuntimeException e) {
            log.error("Ошибка обработки заявки: {}", e.getMessage());
            throw e;  // Здесь поймаете DrPlatformError
        }
    }
}