package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.config.McoProperties;
import org.example.mcoService.dto.request.GetMessageRequest;
import org.example.mcoService.dto.response.DrPlatformError;
import org.example.mcoService.dto.response.GetMessageResponse;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class McoSoapClient {

    private final WebServiceTemplate webServiceTemplate;
    private final McoProperties mcoProperties;
    private final Jaxb2Marshaller marshaller; // Добавлено

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

            log.debug("Получен ответ: {}", response.getClass().getSimpleName());
            return responseClass.cast(response);

        } catch (Exception e) {
            log.error("Ошибка при отправке SOAP запрос", e);
            throw new RuntimeException("Ошибка взаимодействия с API МЧО", e);
        }
    }

    // ============================================
// ИСПРАВЛЕННАЯ ВЕРСИЯ getAsyncResult()
// ЗАМЕНИТЬ в файле McoSoapClient.java
// ============================================

    public <T> T getAsyncResult(String messageId, Class<T> responseClass) throws InterruptedException {
        int maxAttempts = 30;
        int attempt = 0;

        log.info(">>> Начинаем опрос результата по MessageId: {}", messageId);
        log.info("Максимум попыток: {}, интервал: 2 сек, общее время: {} сек",
                maxAttempts, maxAttempts * 2);

        while (attempt < maxAttempts) {
            try {
                attempt++;
                log.debug("⏳ Попытка {}/{} - опрос результата...", attempt, maxAttempts);

                // Создаем GetMessageRequest вручную
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

                // Отправляем
                Object response = webServiceTemplate.sendSourceAndReceive(
                        mcoProperties.getApi().getBaseUrl(),
                        new DOMSource(doc),
                        new WebServiceMessageCallback() {
                            @Override
                            public void doWithMessage(WebServiceMessage message) {
                                if (message instanceof SoapMessage soapMessage) {
                                    soapMessage.setSoapAction("urn:GetMessageRequest");
                                }
                            }
                        },
                        source -> marshaller.unmarshal(source)
                );

                if (response instanceof GetMessageResponse getMessageResponse) {
                    String status = getMessageResponse.getProcessingStatus();
                    log.debug("📊 Статус обработки: {}", status);

                    if ("COMPLETED".equals(status)) {
                        log.info("✅ Запрос обработан успешно за {} попыток ({} сек)",
                                attempt, attempt * 2);

                        if (getMessageResponse.getMessage() != null &&
                                getMessageResponse.getMessage().getContent() != null) {

                            Object content = getMessageResponse.getMessage().getContent();

                            // ============================================
                            // ИСПРАВЛЕНИЕ: Вручную парсим Element в нужный тип
                            // ============================================
                            if (content instanceof Element element) {
                                log.debug("Парсим Element в {}", responseClass.getSimpleName());

                                try {
                                    // Создаем DOMSource из Element
                                    DOMSource source = new DOMSource(element);

                                    // Парсим через JAXB marshaller
                                    Object unmarshalled = marshaller.unmarshal(source);

                                    if (responseClass.isInstance(unmarshalled)) {
                                        log.debug("✅ Успешно распарсили в {}", responseClass.getSimpleName());
                                        return responseClass.cast(unmarshalled);
                                    } else {
                                        throw new RuntimeException(
                                                "Unexpected response type: " + unmarshalled.getClass().getName() +
                                                        ", expected: " + responseClass.getName()
                                        );
                                    }
                                } catch (Exception e) {
                                    log.error("❌ Ошибка парсинга Element: {}", e.getMessage());
                                    throw new RuntimeException("Ошибка парсинга ответа", e);
                                }
                            } else if (responseClass.isInstance(content)) {
                                // Если это уже нужный тип (маловероятно, но проверим)
                                log.debug("Контент уже нужного типа: {}", responseClass.getSimpleName());
                                return responseClass.cast(content);
                            } else {
                                throw new RuntimeException(
                                        "Cannot process content of type: " + content.getClass().getName()
                                );
                            }
                        } else {
                            throw new RuntimeException("Response message is empty");
                        }

                    } else if ("PROCESSING".equals(status)) {
                        log.debug("⏳ Обработка еще не завершена, ожидаем...");
                        Thread.sleep(2000);

                    } else if ("FAILED".equals(status)) {
                        log.error("❌ Запрос завершился с ошибкой");
                        throw new RuntimeException("Processing failed on server");

                    } else {
                        log.warn("⚠️ Неизвестный статус: {}", status);
                        Thread.sleep(2000);
                    }

                } else if (response instanceof DrPlatformError error) {
                    log.error("❌ Получена ошибка от ФНС:");
                    log.error("Код: {}", error.getCode());
                    log.error("Сообщение: {}", error.getMessage());
                    throw new RuntimeException("ФНС вернул ошибку: " + error.getCode() + " - " + error.getMessage());

                } else {
                    log.error("❌ Неожиданный тип ответа: {}", response != null ? response.getClass().getName() : "null");
                    throw new RuntimeException("Unexpected response type");
                }

            } catch (InterruptedException e) {
                log.error("❌ Прервано ожидание результата");
                throw e;

            } catch (RuntimeException e) {
                log.error("❌ Ошибка при опросе результата на попытке {}: {}", attempt, e.getMessage());
                throw new RuntimeException("Ошибка получения результата", e);

            } catch (Exception e) {
                log.error("❌ Непредвиденная ошибка на попытке {}: {}", attempt, e.getMessage(), e);
                throw new RuntimeException("Непредвиденная ошибка при получении результата", e);
            }
        }

        log.error("❌ Превышено время ожидания результата ({} сек)", maxAttempts * 2);
        throw new RuntimeException("Timeout: результат не получен за " + (maxAttempts * 2) + " секунд");
    }
}