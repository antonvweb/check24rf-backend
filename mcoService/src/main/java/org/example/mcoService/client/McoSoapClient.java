package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.config.McoProperties;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpComponents5Connection;

@Slf4j
@Component
@RequiredArgsConstructor
public class McoSoapClient {

    private final WebServiceTemplate webServiceTemplate;
    private final McoProperties mcoProperties;

    /**
     * Универсальный метод для отправки SOAP запросов
     */
    public <T> T sendSoapRequest(Object request, Class<T> responseClass, String soapAction) {
        try {
            log.debug("Отправка SOAP запроса: {}", request.getClass().getSimpleName());
            Object response = webServiceTemplate.marshalSendAndReceive(
                    mcoProperties.getApi().getBaseUrl(),
                    request,
                    message -> {
                        if (message instanceof SoapMessage soapMessage) {
                            // Токен теперь обрабатывается интерсептором, нет необходимости добавлять HTTP-заголовок
                            soapMessage.setSoapAction(soapAction); // Опционально: задайте SOAPAction, если требуется
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
}
