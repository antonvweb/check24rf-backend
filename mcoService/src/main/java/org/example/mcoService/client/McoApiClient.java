package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.request.*;
import org.example.mcoService.dto.response.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class McoApiClient {

    @Autowired
    private McoSoapClient soapClient;

    public GetBindPartnerStatusResponse getBindRequestStatus(List<String> requestIds) {
        log.info("Запрос статуса заявок, количество: {}", requestIds.size());

        if (requestIds.isEmpty()) {
            throw new IllegalArgumentException("Список requestIds не может быть пустым");
        }

        if (requestIds.size() > 50) {
            throw new IllegalArgumentException("Максимум 50 requestIds за один запрос");
        }

        GetBindPartnerStatusRequest innerRequest = GetBindPartnerStatusRequest.builder()
                .requestIds(requestIds)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        return soapClient.sendSoapRequest(
                request,
                GetBindPartnerStatusResponse.class,
                "SendMessageRequest"
        );
    }

    public GetBindPartnerStatusResponse getBindRequestStatusSync(List<String> requestIds) {
        log.info("Синхронный запрос статуса заявок, количество: {}", requestIds.size());

        if (requestIds.isEmpty()) {
            throw new IllegalArgumentException("Список requestIds не может быть пустым");
        }

        if (requestIds.size() > 50) {
            throw new IllegalArgumentException("Максимум 50 requestIds за один запрос");
        }

        GetBindPartnerStatusRequest innerRequest = GetBindPartnerStatusRequest.builder()
                .requestIds(requestIds)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            GetBindPartnerStatusResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    GetBindPartnerStatusResponse.class
            );

            int statusesCount = response.getStatuses() != null ? response.getStatuses().size() : 0;
            log.info("Получено статусов: {}", statusesCount);

            if (response.getStatuses() != null) {
                response.getStatuses().forEach(status -> {
                    log.info("RequestId: {}, Result: {}, UserIdentifier: {}",
                            status.getRequestId(),
                            status.getResult(),
                            status.getUserIdentifier());
                });
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }

    public SendMessageResponse registerPartner(
            String name,
            String description,
            String transitionLink,
            String base64Logo,
            String inn,
            String phone) {

        log.info("Начало регистрации партнера");
        log.info("Имя партнера: [{}]", name);
        log.info("Длина имени: {}", name != null ? name.length() : "null");
        log.info("Тип: PARTNER");
        log.info("ИНН: {}", inn);
        log.info("Телефон: {}", phone);

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

        log.info("Создан объект PostPlatformRegistrationRequest");
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

        SendMessageResponse messageResponse = registerPartner(
                name, description, transitionLink, base64Logo, inn, phone
        );

        log.info("Получен MessageId: {}, ожидаем результата...", messageResponse.getMessageId());

        try {
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

        log.info("Отправляем PostBindPartnerRequest: requestId = {}, userIdentifier = {}",
                innerRequest.getRequestId(), innerRequest.getUserIdentifier());

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        return soapClient.sendSoapRequest(request, SendMessageResponse.class, "SendMessageRequest");
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

                response.getReceipts().forEach(receipt -> log.info("Чек от: {}, источник: {}",
                        receipt.getUserIdentifier(),
                        receipt.getSourceCode()));
            }

            marker = response.getNextMarker();
            hasMore = response.getTotalExpectedRemainingPolls() != null &&
                    response.getTotalExpectedRemainingPolls() > 1;
        }
    }

    public PostBindPartnerResponse bindUserSync(String phoneNumber, String requestId) {
        log.info("Синхронное подключение пользователя: {}", phoneNumber);

        log.info("Формируем PostBindPartnerRequest: requestId = {}, userIdentifier = {}",
                requestId, phoneNumber);

        SendMessageResponse messageResponse = bindUser(phoneNumber, requestId);

        log.info("Получен MessageId: {}, начинаем опрос результата...", messageResponse.getMessageId());

        try {
            PostBindPartnerResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    PostBindPartnerResponse.class
            );

            log.info("Заявка успешно обработана! Ответ: {}", response);
            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        } catch (RuntimeException e) {
            log.error("Ошибка обработки заявки на стороне ФНС: {}", e.getMessage());
            throw e;
        }
    }

    public GetReceiptsTapeResponse getReceiptsSync(String marker) {
        log.info("Синхронное получение ленты чеков с маркером: {}", marker);

        GetReceiptsTapeRequest innerRequest = GetReceiptsTapeRequest.builder()
                .marker(marker != null ? marker : "S_FROM_END")
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            GetReceiptsTapeResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    GetReceiptsTapeResponse.class
            );

            int receiptsCount = response.getReceipts() != null ? response.getReceipts().size() : 0;
            log.info("Получено чеков: {}", receiptsCount);

            if (response.getNextMarker() != null) {
                log.debug("NextMarker: {}", response.getNextMarker());
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прервано ожидание результата", e);
            throw new RuntimeException("Прервано ожидание результата", e);
        } catch (RuntimeException e) {
            log.error("Ошибка получения чеков: {}", e.getMessage());
            throw e;
        }
    }

    public void getAllReceiptsSync() {
        log.info("Начало получения всех чеков");

        String marker = "S_FROM_END";
        int totalReceipts = 0;
        int iteration = 0;
        boolean hasMore = true;
        int maxIterations = 50;

        while (hasMore && iteration < maxIterations) {
            iteration++;
            log.info("Итерация {}", iteration);

            try {
                GetReceiptsTapeResponse response = getReceiptsSync(marker);

                if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
                    int batchSize = response.getReceipts().size();
                    totalReceipts += batchSize;
                    log.info("Получено чеков в этой порции: {}", batchSize);
                } else {
                    log.info("Чеков в этой порции нет");
                }

                if (response.getNextMarker() != null && !response.getNextMarker().isEmpty()) {
                    marker = response.getNextMarker();
                    log.debug("NextMarker для следующей итерации: {}", marker);
                } else {
                    log.info("NextMarker отсутствует - это была последняя порция");
                    hasMore = false;
                }

                Long remainingPolls = response.getTotalExpectedRemainingPolls();
                if (remainingPolls != null) {
                    log.info("Осталось порций для загрузки: {}", remainingPolls);
                    hasMore = hasMore && (remainingPolls > 0);
                }

                if (hasMore) {
                    Thread.sleep(500);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Прервано получение чеков на итерации {}", iteration);
                break;
            } catch (Exception e) {
                log.error("Ошибка при получении чеков на итерации {}", iteration, e);
                break;
            }
        }

        if (iteration >= maxIterations) {
            log.warn("Достигнуто максимальное количество итераций ({}) - остановка", maxIterations);
        }

        log.info("Завершено: Всего получено {} чеков за {} итераций",
                totalReceipts, iteration);
    }

    public PostBindPartnerBatchResponse bindUsersBatch(
            String requestId,
            List<String> phoneNumbers) {

        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            throw new IllegalArgumentException("Список телефонов не может быть пустым");
        }
        if (phoneNumbers.size() > 15000) {
            throw new IllegalArgumentException("Максимум 15000 телефонов за один запрос");
        }

        log.info("Пакетное подключение {} пользователей", phoneNumbers.size());

        PostBindPartnerBatchRequest innerRequest = PostBindPartnerBatchRequest.builder()
                .requestId(requestId)
                .userIdentifiers(phoneNumbers)
                .permissionGroups(Collections.singletonList("DEFAULT"))
                .isUnverifiedIdentifier(false)
                .requireNoActiveRequests(false)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            PostBindPartnerBatchResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    PostBindPartnerBatchResponse.class
            );

            int accepted = response.getAcceptedUserIdentifiers() != null ?
                    response.getAcceptedUserIdentifiers().size() : 0;
            int rejected = response.getRejectedUserIdentifiers() != null ?
                    response.getRejectedUserIdentifiers().size() : 0;

            log.info("Принято: {}, Отклонено: {}", accepted, rejected);

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }

    public GetBindPartnerEventResponse getBindPartnerEvents(String marker) {

        log.info("Запрос событий с маркером: {}", marker);

        GetBindPartnerEventRequest innerRequest = GetBindPartnerEventRequest.builder()
                .marker(marker)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            GetBindPartnerEventResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    GetBindPartnerEventResponse.class
            );

            int eventsCount = response.getEvents() != null ? response.getEvents().size() : 0;
            log.info("Получено событий: {}, Новый маркер: {}",
                    eventsCount, response.getMarker());

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }

    public GetUnboundPartnerResponse getUnboundPartners(String marker) {

        log.info("Запрос отключившихся пользователей с маркером: {}", marker);

        GetUnboundPartnerRequest innerRequest = GetUnboundPartnerRequest.builder()
                .marker(marker)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            GetUnboundPartnerResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    GetUnboundPartnerResponse.class
            );

            int unboundsCount = response.getUnbounds() != null ? response.getUnbounds().size() : 0;
            log.info("Отключившихся: {}, HasMore: {}, NextMarker: {}",
                    unboundsCount, response.getHasMore(), response.getNextMarker());

            if (response.getUnbounds() != null) {
                response.getUnbounds().forEach(unbound -> {
                    log.info("UserIdentifier: {}, RequestId: {}, ResponseTime: {}",
                            unbound.getUserIdentifier(),
                            unbound.getRequestId(),
                            unbound.getResponseTime());
                });
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }

    public PostNotificationResponse sendNotification(
            String requestId,
            String phoneNumber,
            String title,
            String message,
            String shortMessage,
            String category,
            String externalItemId,
            String externalItemUrl) {

        log.info("Отправка уведомления пользователю: {}, категория: {}", phoneNumber, category);

        PostNotificationRequest innerRequest = PostNotificationRequest.builder()
                .requestId(requestId)
                .userIdentifier(phoneNumber)
                .notificationTitle(title)
                .notificationMessage(message)
                .shortMessage(shortMessage)
                .notificationCategory(category)
                .externalItemId(externalItemId)
                .externalItemUrl(externalItemUrl)
                .build();

        SendMessageRequest request = SendMessageRequest.builder()
                .message(new SendMessageRequest.MessageWrapper(innerRequest))
                .build();

        SendMessageResponse messageResponse = soapClient.sendSoapRequest(
                request,
                SendMessageResponse.class,
                "SendMessageRequest"
        );

        log.info("Запрос отправлен, MessageId: {}, опрашиваем результат...",
                messageResponse.getMessageId());

        try {
            PostNotificationResponse response = soapClient.getAsyncResult(
                    messageResponse.getMessageId(),
                    PostNotificationResponse.class
            );

            log.info("Уведомление отправлено, RequestId: {}, HandledAt: {}",
                    response.getRequestId(), response.getHandledAt());

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано ожидание результата", e);
        }
    }
}