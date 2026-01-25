package org.example.mcoService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.client.McoSoapClient;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.request.PostNotificationRequest;
import org.example.mcoService.dto.request.PostUnbindPartnerRequest;
import org.example.mcoService.dto.response.*;
import org.example.mcoService.entity.UserBindingStatus;
import org.example.mcoService.exception.BusinessMcoException;
import org.example.mcoService.exception.FatalMcoException;
import org.example.mcoService.exception.McoErrorCode;
import org.example.mcoService.exception.McoException;
import org.example.mcoService.repository.UserBindingStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class McoService {

    private final McoApiClient apiClient;
    private final McoProperties properties;
    private final McoApiClient mcoApiClient;
    private final ReceiptService receiptService;
    private final McoSoapClient soapClient;
    private final UserBindingStatusRepository bindingStatusRepository;

    public GetUnboundPartnerResponse getUnboundPartners(String marker) {
        return mcoApiClient.getUnboundPartners(marker);
    }

    // Добавлена проверка статуса привязки перед отправкой уведомлений
    public PostNotificationResponse sendNotification(
            String requestId,
            String phoneNumber,
            String title,
            String message,
            String shortMessage,
            String category,
            String externalItemId,
            String externalItemUrl
    ) {
        UserBindingStatus bindingStatus = bindingStatusRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessMcoException(McoErrorCode.USER_NOT_BOUND, "Пользователь не найден или не привязан"));

        if (!bindingStatus.isBound()) {
            throw new BusinessMcoException(McoErrorCode.USER_NOT_BOUND, "User is not bound");
        }

        PostNotificationRequest request = PostNotificationRequest.builder()
                .requestId(requestId)
                .userIdentifier(phoneNumber)
                .notificationTitle(title)
                .notificationMessage(message)
                .shortMessage(shortMessage)
                .notificationCategory(category)
                .externalItemUrl(externalItemUrl)
                .externalItemId(externalItemId)
                .build();

        return soapClient.sendSoapRequest(
                request,
                PostNotificationResponse.class,
                "PostNotificationRequest",
                UUID.randomUUID().toString(),
                requestId
        );
    }

    public GetBindPartnerStatusResponse.BindPartnerStatus checkBindRequestStatus(String requestId) {
        log.info("Проверка статуса заявки: {}", requestId);

        GetBindPartnerStatusResponse response = apiClient.getBindRequestStatusSync(
                Collections.singletonList(requestId)
        );

        if (response.getStatuses() == null || response.getStatuses().isEmpty()) {
            throw new BusinessMcoException(
                    McoErrorCode.REQUEST_NOT_FOUND,
                    "Статус заявки не найден или заявка ещё не обработана"
            );
        }

        GetBindPartnerStatusResponse.BindPartnerStatus status = response.getStatuses().get(0);

        // Дополнительная защита от несоответствия requestId (на всякий случай)
        if (!requestId.equals(status.getRequestId())) {
            throw new FatalMcoException(
                    McoErrorCode.UNKNOWN,
                    "Несоответствие requestId в ответе: ожидался " + requestId + ", получен " + status.getRequestId()
            );
        }

        return status;
    }

    public List<GetBindPartnerStatusResponse.BindPartnerStatus> checkBindRequestStatuses(List<String> requestIds) {
        log.info("Проверка статусов заявок, количество: {}", requestIds.size());

        if (requestIds.isEmpty()) {
            throw new BusinessMcoException(
                    McoErrorCode.REQUEST_VALIDATION_ERROR,
                    "Validation error"
            );
        }

        if (requestIds.size() > 50) {
            throw new BusinessMcoException(
                    McoErrorCode.TOO_MANY_REQUEST_IDS,
                    "Максимум 50 requestIds за один запрос"
            );
        }

        GetBindPartnerStatusResponse response = apiClient.getBindRequestStatusSync(requestIds);

        List<GetBindPartnerStatusResponse.BindPartnerStatus> statuses =
                response.getStatuses() != null ? response.getStatuses() : Collections.emptyList();

        // Проверяем, что количество полученных статусов соответствует запрошенному
        if (statuses.size() < requestIds.size()) {
            log.warn("Получено только {} статусов из {} запрошенных", statuses.size(), requestIds.size());
            // Можно выбросить исключение или вернуть частичный результат
            // Здесь выбрасываем бизнес-исключение для прозрачности
            throw new BusinessMcoException(
                    McoErrorCode.PARTIAL_REQUESTS_NOT_FOUND,
                    String.format("Получено только %d статусов из %d запрошенных (некоторые заявки не найдены)",
                            statuses.size(), requestIds.size())
            );
        }

        return statuses;
    }

    public String checkUserBindStatus(String phone) {
        log.warn("Метод checkUserBindStatus требует сохранения requestId при создании заявки");
        return "BOUND";
    }

    public String initializePartner(String logoPath) {
        try {
            byte[] logoBytes = Files.readAllBytes(Path.of(logoPath));
            if (logoBytes.length > 100 * 1024) {
                throw new BusinessMcoException(
                        McoErrorCode.LOGO_TOO_LARGE,
                        "Размер логотипа превышает 100 КБ"
                );
            }

            String mimeType = Files.probeContentType(Path.of(logoPath));
            if (!"image/jpeg".equals(mimeType)) {
                throw new BusinessMcoException(
                        McoErrorCode.LOGO_INVALID_FORMAT,
                        "Логотип должен быть в формате JPEG"
                );
            }

            String base64Logo = Base64.getEncoder().encodeToString(logoBytes);

            PostPlatformRegistrationResponse response = apiClient.registerPartnerSync(
                    properties.getPartner().getName(),
                    "Платформа для начисления бонусов и кешбэка",
                    "https://xn--24-mlcu7d.xn--p1ai/",
                    base64Logo,
                    properties.getPartner().getInn(),
                    "79991234567"
            );

            log.info("Партнер успешно зарегистрирован, ID: {}", response.getId());
            return response.getId();

        } catch (IOException e) {
            log.error("Ошибка чтения файла логотипа: {}", logoPath, e);
            throw new BusinessMcoException(
                    McoErrorCode.LOGO_PROCESSING_ERROR,
                    "Не удалось прочитать файл логотипа"
            );
        }
    }

    @Transactional
    public String connectUser(String phone) {
        String requestId = UUID.randomUUID().toString().toUpperCase();

        log.info("Создание заявки на подключение пользователя {}, RequestId: {}", phone, requestId);

        // Сначала создаем запись в UserBindingStatus со статусом PENDING
        UserBindingStatus initialStatus = UserBindingStatus.builder()
                .phoneNumber(phone)
                .requestId(requestId)
                .bindingStatus(UserBindingStatus.BindingStatus.PENDING)
                .partnerConnected(false)
                .receiptsEnabled(false)
                .notificationsEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .bound(false)
                .build();

        bindingStatusRepository.save(initialStatus);
        log.info("Создана начальная запись в user_binding_status для запроса {}", requestId);

        PostBindPartnerResponse response = apiClient.bindUserSync(phone, requestId);

        log.info("Заявка на подключение отправлена");
        log.info("MessageId: {}", response.getMessageId());

        return requestId;
    }

    @Transactional
    public void unbindUser(String phoneNumber, String unbindReason) {
        log.info("Отключение пользователя: {}", phoneNumber);

        UserBindingStatus status = bindingStatusRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessMcoException(
                        McoErrorCode.USER_NOT_BOUND,
                        "User not bound to partner"
                ));

        if (status.getBindingStatus() == UserBindingStatus.BindingStatus.UNBOUND) {
            log.info("Пользователь уже отключен локально: {}", phoneNumber);
            return;
        }

        PostUnbindPartnerRequest request = PostUnbindPartnerRequest.builder()
                .userIdentifier(phoneNumber)
                .unbindReason(unbindReason)
                .build();

        try {
            PostUnbindPartnerResponse response = soapClient.sendSoapRequest(
                    request,
                    PostUnbindPartnerResponse.class,
                    "PostUnbindPartnerRequest"
            );

            if ("OK".equals(response.getStatus())) {
                updateBindingStatus(status);
                log.info("Пользователь успешно отключен на стороне МЧО: {}", phoneNumber);
            } else {
                throw new FatalMcoException(
                        McoErrorCode.UNKNOWN,
                        "Неожиданный статус ответа от сервера при отключении: " + response.getStatus()
                );
            }

        } catch (McoException e) {
            // бизнес-ошибки и retryable пробрасываем дальше — их обработает контроллер
            throw e;
        } catch (RuntimeException e) {
            // ловим только явные случаи уже отвязанного пользователя от сервера
            if (e.getCause() instanceof McoException cause &&
                    McoErrorCode.IDENTIFIER_UNBOUND.equals(cause.getErrorCode())) {
                log.info("Пользователь уже отвязан на стороне МЧО: {}", phoneNumber);
                updateBindingStatus(status);
                return;
            }
            throw e;
        }
    }

    private void updateBindingStatus(UserBindingStatus status) {
        status.setBindingStatus(UserBindingStatus.BindingStatus.UNBOUND);
        status.setPartnerConnected(false);
        status.setReceiptsEnabled(false);
        status.setNotificationsEnabled(false);
        status.setUnboundAt(LocalDateTime.now());
        bindingStatusRepository.save(status);
    }

    public PostBindPartnerBatchResponse bindUsersBatch(String requestId, List<String> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            throw new BusinessMcoException(
                    McoErrorCode.REQUEST_VALIDATION_ERROR,
                    "Список телефонов не может быть пустым"
            );
        }

        if (phoneNumbers.size() > 15000) {
            throw new BusinessMcoException(
                    McoErrorCode.BATCH_TOO_LARGE,
                    "Максимум 15000 телефонов за один запрос"
            );
        }

        return mcoApiClient.bindUsersBatch(requestId, phoneNumbers);
    }

    public GetBindPartnerEventResponse getBindPartnerEvents(String marker) {
        return mcoApiClient.getBindPartnerEvents(marker);
    }

    public void syncReceipts() {
        log.info("Начало полной синхронизации чеков");
        apiClient.getAllReceiptsSync();
        log.info("Синхронизация завершена");
    }

    public int testReceiptsOnce() {
        log.info("Тестовое получение чеков");

        try {
            GetReceiptsTapeResponse response = apiClient.getReceiptsSync("S_FROM_END");

            if (response.getReceipts() == null || response.getReceipts().isEmpty()) {
                log.warn("Чеков не найдено");
                return 0;
            }

            log.info("Успешно получено {} чеков", response.getReceipts().size());

            var firstReceipt = response.getReceipts().get(0);
            log.info("Пример чека:");
            log.info("Пользователь: {}", firstReceipt.getUserIdentifier());
            log.info("Дата: {}", firstReceipt.getReceiveDate());
            log.info("Источник: {}", firstReceipt.getSourceCode());

            return response.getReceipts().size();

        } catch (Exception e) {
            log.error("Ошибка при тестировании получения чеков", e);
            throw new RuntimeException("Ошибка получения чеков: " + e.getMessage(), e);
        }
    }

    public GetReceiptsTapeResponse getReceiptsByMarker(String marker) {
        log.info("Получение чеков по маркеру: {}", marker);

        GetReceiptsTapeResponse response = apiClient.getReceiptsSync(marker);

        if (response.getReceipts() != null && !response.getReceipts().isEmpty()) {
            int savedCount = receiptService.saveReceipts(response.getReceipts());
            log.info("Автоматически сохранено {} новых чеков в БД", savedCount);
        }

        return response;
    }

    public String getReceiptsStats() {
        try {
            GetReceiptsTapeResponse response = apiClient.getReceiptsSync("S_FROM_END");

            int receiptsCount = response.getReceipts() != null ? response.getReceipts().size() : 0;
            Long remainingPolls = response.getTotalExpectedRemainingPolls();

            return String.format(
                    "Статистика чеков:\n" +
                            "Получено в текущей порции: %d\n" +
                            "Осталось порций для загрузки: %d\n" +
                            "NextMarker: %s",
                    receiptsCount,
                    remainingPolls != null ? remainingPolls : 0,
                    response.getNextMarker()
            );

        } catch (Exception e) {
            log.error("Ошибка получения статистики", e);
            return "Ошибка: " + e.getMessage();
        }
    }
}
