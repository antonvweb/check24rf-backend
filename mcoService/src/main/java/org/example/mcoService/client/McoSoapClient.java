package org.example.mcoService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.config.McoProperties;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;

import javax.xml.namespace.QName;

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

            // Interceptor для добавления токена в заголовок
            ClientInterceptor tokenInterceptor = new ClientInterceptor() {
                @Override
                public boolean handleRequest(MessageContext messageContext) {
                    if (messageContext.getRequest() instanceof SoapMessage soapMessage) {
                        try {
                            soapMessage.getSoapHeader()
                                    .addHeaderElement(new QName(
                                            "urn://x-artefacts-gnivc-ru/ais3/kkt/DrPartnersIntegrationService/v0.1",
                                            "FNS-OpenApi-Token"
                                    ))
                                    .setText(mcoProperties.getApi().getToken());
                        } catch (Exception e) {
                            log.error("Ошибка добавления токена в SOAP-заголовок", e);
                        }
                    }
                    return true;
                }

                @Override
                public boolean handleResponse(MessageContext messageContext) {
                    return true;
                }

                @Override
                public boolean handleFault(MessageContext messageContext) {
                    return true;
                }

                @Override
                public void afterCompletion(MessageContext messageContext, Exception ex) {
                    // Здесь можно очистить ресурсы или логировать окончание запроса
                    if (ex != null) {
                        log.error("Ошибка в afterCompletion SOAP запроса", ex);
                    }
                }
            };

            webServiceTemplate.setInterceptors(new ClientInterceptor[]{tokenInterceptor});

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
