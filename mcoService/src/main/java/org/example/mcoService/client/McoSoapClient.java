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
            log.error("Ошибка при отправке SOAP запроса", e);
            throw new RuntimeException("Ошибка взаимодействия с API МЧО", e);
        }
    }

    public <T> T getAsyncResult(String messageId, Class<T> responseClass) throws InterruptedException {
        int maxAttempts = 10;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                log.debug("Опрос результата по MessageId: {}, попытка {}", messageId, attempt + 1);

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
                    if ("COMPLETED".equals(getMessageResponse.getProcessingStatus())) {
                        if (getMessageResponse.getMessage() != null &&
                                getMessageResponse.getMessage().getContent() != null) {

                            Object content = getMessageResponse.getMessage().getContent();

                            // Проверяем на ошибку
                            if (content instanceof DrPlatformError error) {
                                String errorMsg = String.format("Ошибка API МЧО: [%s] %s",
                                        error.getCode(), error.getMessage());
                                log.error(errorMsg);
                                throw new RuntimeException(errorMsg);
                            }

                            return responseClass.cast(content);
                        }
                    } else if ("FAILED".equals(getMessageResponse.getProcessingStatus())) {
                        throw new RuntimeException("Обработка запроса завершилась с ошибкой");
                    }
                }

                // Ждем 2 секунды перед следующей попыткой
                Thread.sleep(2000);
                attempt++;

            } catch (Exception e) {
                log.error("Ошибка при опросе результата", e);
                throw new RuntimeException("Ошибка получения результата", e);
            }
        }

        throw new RuntimeException("Превышено время ожидания результата");
    }
}