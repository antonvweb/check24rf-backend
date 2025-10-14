package org.example.mcoService.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;

import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SoapConfig {

    private final McoProperties mcoProperties;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.example.mcoService.dto");
        return marshaller;
    }

    @Bean
    public ClientInterceptor tokenInterceptor() {
        return new ClientInterceptor() {
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
                        log.debug("Токен добавлен в SOAP заголовок");
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
            public void afterCompletion(MessageContext messageContext, Exception ex) { }
        };
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(
            Jaxb2Marshaller marshaller,
            HttpComponents5MessageSender messageSender,
            ClientInterceptor tokenInterceptor) {

        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(messageSender);
        template.setInterceptors(new ClientInterceptor[]{tokenInterceptor});

        return template;
    }

    @Bean
    public HttpClient httpClient() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true) // доверяем всем сертификатам (только для DEV)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE
            );

            Registry<org.apache.hc.client5.http.socket.ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<org.apache.hc.client5.http.socket.ConnectionSocketFactory>create()
                            .register("https", sslSocketFactory)
                            .register("http", PlainConnectionSocketFactory.getSocketFactory())
                            .build();

            // ✅ Современный способ конфигурации пула соединений
            PoolingHttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(sslSocketFactory)
                            .setMaxConnTotal(100)
                            .setMaxConnPerRoute(20)
                            .setDefaultConnectionConfig(
                                    org.apache.hc.client5.http.config.ConnectionConfig.custom()
                                            .setConnectTimeout(Timeout.ofSeconds(30))
                                            .setSocketTimeout(Timeout.ofSeconds(60))
                                            .build()
                            )
                            .build();

            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .evictExpiredConnections()
                    .evictIdleConnections(TimeValue.of(5, TimeUnit.MINUTES))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания HTTP клиента", e);
        }
    }

    @Bean
    public HttpComponents5MessageSender httpComponents5MessageSender(HttpClient httpClient) {
        // Таймауты уже заданы в самом HttpClient
        return new HttpComponents5MessageSender(httpClient);
    }
}