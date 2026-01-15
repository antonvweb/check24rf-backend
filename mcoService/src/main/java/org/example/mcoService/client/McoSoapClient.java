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
// ЗАМЕНИТЕ МЕТОД getAsyncResult() в McoSoapClient.java
// ============================================

    public <T> T getAsyncResult(String messageId, Class<T> responseClass) throws InterruptedException {
        // УВЕЛИЧЕНО: было 10 попыток (20 сек), стало 30 попыток (60 сек)
        int maxAttempts = 30;
        int attempt = 0;

        log.info(">>> Начинаем опрос результата по MessageId: {}", messageId);
        log.info("Максимум попыток: {}, интервал: 2 сек, общее время: {} сек",
                maxAttempts, maxAttempts * 2);

        while (attempt < maxAttempts) {
            try {
                attempt++;
                log.debug("⏳ Попытка {}/{} - опрос результата...", attempt, maxAttempts);

                GetMessageRequest request = GetMessageRequest.builder()
                        .messageId(messageId)
                        .build();

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

                            // Проверяем на ошибку
                            if (content instanceof DrPlatformError error) {
                                String errorMsg = String.format("Ошибка API МЧО: [%s] %s",
                                        error.getCode(), error.getMessage());
                                log.error("❌ {}", errorMsg);
                                throw new RuntimeException(errorMsg);
                            }

                            return responseClass.cast(content);
                        } else {
                            log.warn("⚠️ COMPLETED но нет content в ответе");
                        }

                    } else if ("FAILED".equals(status)) {
                        log.error("❌ Обработка запроса завершилась с ошибкой");
                        throw new RuntimeException("Обработка запроса завершилась с ошибкой");

                    } else if ("PROCESSING".equals(status)) {
                        log.debug("⏳ Обработка еще не завершена, ожидаем...");
                        // Продолжаем опрос
                    } else {
                        log.warn("⚠️ Неизвестный статус: {}", status);
                    }
                }

                // Ждем 2 секунды перед следующей попыткой
                if (attempt < maxAttempts) {
                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {
                log.error("❌ Прервано ожидание на попытке {}", attempt);
                throw e;
            } catch (Exception e) {
                log.error("❌ Ошибка при опросе результата на попытке {}: {}",
                        attempt, e.getMessage());
                throw new RuntimeException("Ошибка получения результата", e);
            }
        }

        log.error("❌ ПРЕВЫШЕНО ВРЕМЯ ОЖИДАНИЯ!");
        log.error("Выполнено {} попыток за {} секунд", maxAttempts, maxAttempts * 2);
        log.error("MessageId: {}", messageId);
        log.error("");
        log.error("ВОЗМОЖНЫЕ ПРИЧИНЫ:");
        log.error("  1. Запрос обрабатывается слишком долго (много данных)");
        log.error("  2. Проблема на стороне сервера МЧО");
        log.error("  3. Неправильный формат запроса");
        log.error("");
        log.error("ЧТО ДЕЛАТЬ:");
        log.error("  1. Проверьте сохраненные XML файлы (soap-request-*.xml и soap-response-*.xml)");
        log.error("  2. Попробуйте запрос позже");
        log.error("  3. Обратитесь в поддержку МЧО если проблема повторяется");

        throw new RuntimeException("Превышено время ожидания результата (попыток: " +
                maxAttempts + ", время: " + (maxAttempts * 2) + " сек)");
    }
}