package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.response.DrPlatformError;
import org.example.mcoService.dto.response.GetMessageResponse;
import org.example.mcoService.exception.*;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;


@Slf4j
@Component
@RequiredArgsConstructor
public class McoSoapClient {

    private final WebServiceTemplate webServiceTemplate;
    private final McoProperties mcoProperties;
    private final Jaxb2Marshaller marshaller;

    public <T> T sendSoapRequest(Object request, Class<T> responseClass, String soapAction) {
        try {
            log.debug("Отправка SOAP запроса: {}", request.getClass().getSimpleName());

            Object response = webServiceTemplate.marshalSendAndReceive(
                    mcoProperties.getApi().getBaseUrl(),
                    request,
                    message -> {
                        if (message instanceof SoapMessage soapMessage) {
                            soapMessage.setSoapAction("urn:" + soapAction);
                            log.debug("SOAPAction установлен: urn:{}", soapAction);
                        }
                    }
            );

            log.debug("Получен ответ: {}", response != null ? response.getClass().getSimpleName() : "null");

            if (response instanceof DrPlatformError error) {
                log.error("Синхронная ошибка от ФНС в sendSoapRequest: code = {}, message = {}",
                        error.getCode(), error.getMessage());
                // Уточнение вызова метода для устранения неоднозначности
                GlobalExceptionHandler.processDrPlatformError(error);
                throw new AssertionError("handleDrPlatformError должен был выбросить исключение");
            }

            if (response == null) {
                throw new RuntimeException("Ответ от сервера пустой");
            }

            return responseClass.cast(response);

        } catch (McoException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при отправке SOAP запроса (soapAction: {}): {}", soapAction, e.getMessage(), e);
            throw new RuntimeException("Ошибка взаимодействия с API МЧО", e);
        }
    }

    // Добавлен метод для обработки messageId и requestId для идемпотентности
    public <T> T sendSoapRequest(Object request, Class<T> responseClass, String soapAction, String messageId, String requestId) {
        try {
            log.debug("Отправка SOAP запроса: {}", request.getClass().getSimpleName());

            Object response = webServiceTemplate.marshalSendAndReceive(
                    mcoProperties.getApi().getBaseUrl(),
                    request,
                    message -> {
                        if (message instanceof SoapMessage soapMessage) {
                            soapMessage.setSoapAction("urn:" + soapAction);
                            soapMessage.getSoapHeader().addHeaderElement(new QName("messageId")).setText(messageId);
                            soapMessage.getSoapHeader().addHeaderElement(new QName("requestId")).setText(requestId);
                            log.debug("SOAPAction установлен: urn:{}, messageId: {}, requestId: {}", soapAction, messageId, requestId);
                        }
                    }
            );

            log.debug("Получен ответ: {}", response != null ? response.getClass().getSimpleName() : "null");

            if (response instanceof DrPlatformError error) {
                log.error("Синхронная ошибка от ФНС в sendSoapRequest: code = {}, message = {}", error.getCode(), error.getMessage());
                throw mapDrPlatformErrorToException(error);
            }

            return responseClass.cast(response);
        } catch (Exception e) {
            log.error("Ошибка при отправке SOAP запроса", e);
            throw new FatalMcoException("Ошибка при отправке SOAP запроса", McoErrorCode.REQUEST_VALIDATION_ERROR);
        }
    }

    private RuntimeException mapDrPlatformErrorToException(DrPlatformError error) {
        switch (error.getCode()) {
            case "OPENAPI_PARTNER_API_INTERNAL_ERROR" -> {
                return new RetryableMcoException(error.getMessage(), McoErrorCode.OPENAPI_PARTNER_API_INTERNAL_ERROR);
            }
            case "REQUEST_VALIDATION_ERROR" -> {
                return new BusinessMcoException(error.getMessage(), McoErrorCode.REQUEST_VALIDATION_ERROR);
            }
            default -> {
                return new FatalMcoException(error.getMessage(), McoErrorCode.OPENAPI_PARTNER_API_PARTNER_DENY);
            }
        }
    }

    public <T> T getAsyncResult(String messageId, Class<T> responseClass) throws InterruptedException {
        int maxAttempts = 30;
        int attempt = 0;
        log.info("Начинаем опрос результата по MessageId: {}", messageId);
        log.info("Максимум попыток: {}, интервал: 2 сек, общее время: {} сек",
                maxAttempts, maxAttempts * 2);
        while (attempt < maxAttempts) {
            try {
                attempt++;
                log.debug("Попытка {}/{} - опрос результата...", attempt, maxAttempts);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();
                Element getMessageRequest = doc.createElementNS(
                        "urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0",
                        "GetMessageRequest"
                );
                doc.appendChild(getMessageRequest);
                Element messageIdElement = doc.createElementNS(
                        "urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0",
                        "MessageId"
                );
                messageIdElement.setTextContent(messageId);
                getMessageRequest.appendChild(messageIdElement);
                Object response = webServiceTemplate.sendSourceAndReceive(
                        mcoProperties.getApi().getBaseUrl(),
                        new DOMSource(doc),
                        message -> {
                            if (message instanceof SoapMessage soapMessage) {
                                soapMessage.setSoapAction("urn:GetMessageRequest");
                            }
                        },
                        marshaller::unmarshal
                );
                if (response instanceof GetMessageResponse getMessageResponse) {
                    String status = getMessageResponse.getProcessingStatus();
                    log.debug("Статус обработки: {}", status);
                    if ("COMPLETED".equals(status)) {
                        log.info("Запрос обработан успешно за {} попыток ({} сек)",
                                attempt, attempt * 2);
                        if (getMessageResponse.getMessage() != null &&
                                getMessageResponse.getMessage().getContent() != null) {
                            Object content = getMessageResponse.getMessage().getContent();
                            if (content instanceof Element element) {
                                log.debug("Парсим Element в {}", responseClass.getSimpleName());
                                try {
                                    DOMSource source = new DOMSource(element);
                                    Object unmarshalled = marshaller.unmarshal(source);
                                    if (responseClass.isInstance(unmarshalled)) {
                                        log.debug("Успешно распарсили в {}", responseClass.getSimpleName());
                                        return responseClass.cast(unmarshalled);
                                    } else {
                                        throw new RuntimeException(
                                                "Неожиданный тип ответа: " + unmarshalled.getClass().getName() +
                                                        ", ожидался: " + responseClass.getName()
                                        );
                                    }
                                } catch (Exception e) {
                                    log.error("Ошибка парсинга Element: {}", e.getMessage());
                                    throw new RuntimeException("Ошибка парсинга ответа", e);
                                }
                            } else if (responseClass.isInstance(content)) {
                                log.debug("Контент уже нужного типа: {}", responseClass.getSimpleName());
                                return responseClass.cast(content);
                            } else {
                                throw new RuntimeException(
                                        "Не удается обработать контент типа: " + content.getClass().getName()
                                );
                            }
                        } else {
                            throw new RuntimeException("Ответ пуст");
                        }
                    } else if ("PROCESSING".equals(status)) {
                        log.debug("Обработка еще не завершена, ожидаем...");
                        Thread.sleep(2000);
                    } else if ("FAILED".equals(status)) {
                        log.error("Запрос завершился с ошибкой");
                        throw new RuntimeException("Обработка завершилась ошибкой на сервере");
                    } else {
                        log.warn("Неизвестный статус: {}", status);
                        Thread.sleep(2000);
                    }
                } else if (response instanceof DrPlatformError error) {
                    log.error("Получена ошибка от ФНС: code = {}, message = {}", error.getCode(), error.getMessage());
                    GlobalExceptionHandler.processDrPlatformError(error);
                    throw new AssertionError("handleDrPlatformError должен был выбросить исключение");
                } else {
                    log.error("Неожиданный тип ответа: {}", response != null ? response.getClass().getName() : "null");
                    throw new RuntimeException("Неожиданный тип ответа");
                }
            } catch (InterruptedException e) {
                log.error("Прервано ожидание результата");
                Thread.currentThread().interrupt();
                throw e;
            } catch (McoException e) {
                throw e;
            } catch (RuntimeException e) {
                log.error("Ошибка при опросе результата на попытке {}: {}", attempt, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Непредвиденная ошибка на попытке {}: {}", attempt, e.getMessage(), e);
                throw new RuntimeException("Непредвиденная ошибка при получении результата", e);
            }
        }
        log.error("Превышено время ожидания результата ({} сек)", maxAttempts * 2);
        throw new RetryableMcoException(
                "Таймаут ожидания результата от МЧО после " + (maxAttempts * 2) + " секунд",
                McoErrorCode.OPENAPI_PARTNER_API_INTERNAL_ERROR
        );
    }

    public void someMethod() {
        throw new FatalMcoException("Ошибка при отправке SOAP запроса", McoErrorCode.OPENAPI_PARTNER_API_PARTNER_DENY);
    }

    public void handleError(String errorCode, String errorMessage) {
        switch (errorCode) {
            case "OPENAPI_PARTNER_API_INTERNAL_ERROR" -> new RetryableMcoException(errorMessage, McoErrorCode.OPENAPI_PARTNER_API_INTERNAL_ERROR);
            case "BUSINESS_ERROR" -> new BusinessMcoException(errorMessage, McoErrorCode.REQUEST_VALIDATION_ERROR);
            default -> new FatalMcoException("Неизвестная ошибка", McoErrorCode.OPENAPI_PARTNER_API_PARTNER_DENY);
        }
    }
}