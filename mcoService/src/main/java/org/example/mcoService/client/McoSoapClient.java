package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.config.McoProperties;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class McoSoapClient {

    private WebServiceTemplate webServiceTemplate;
    private McoProperties mcoProperties;

    /**
     * Универсальный метод для отправки SOAP запросов
     */
    public <T> T sendSoapRequest(Object request, Class<T> responseClass, String soapAction) {
        try {
            log.debug("Отправка SOAP запроса: {}", request.getClass().getSimpleName());

            // Просто используем уже настроенный бин
            Object response = webServiceTemplate.marshalSendAndReceive(
                    mcoProperties.getApi().getBaseUrl(),
                    request
            );

            log.debug("Получен ответ: {}", response.getClass().getSimpleName());
            return responseClass.cast(response);

        } catch (Exception e) {
            log.error("Ошибка при отправке SOAP запроса", e);
            throw new RuntimeException("Ошибка взаимодействия с API МЧО", e);
        }
    }
}
