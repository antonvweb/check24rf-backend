package org.example.mcoService.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

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
                    } catch (Exception e) {
                        log.error("Ошибка добавления токена в SOAP-заголовок", e);
                    }
                }
                return true;
            }
            @Override public boolean handleResponse(MessageContext messageContext) { return true; }
            @Override public boolean handleFault(MessageContext messageContext) { return true; }
            @Override public void afterCompletion(MessageContext messageContext, Exception ex) { }
        };
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(HttpComponentsMessageSender messageSender) throws Exception {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller());
        template.setUnmarshaller(marshaller());
        template.setMessageSender(messageSender); // теперь Spring подставит bean
        template.setInterceptors(new ClientInterceptor[]{tokenInterceptor()});
        return template;
    }


    @Bean(destroyMethod = "shutdown")
    public PoolingHttpClientConnectionManager connectionManager() {
        return new PoolingHttpClientConnectionManager();
    }

    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager cm) throws Exception {
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                (hostname, session) -> true
        );

        return HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .setConnectionManager(cm)
                .disableContentCompression()
                .build();
    }

    @Bean
    public HttpComponentsMessageSender httpComponentsMessageSender(CloseableHttpClient httpClient) {
        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        sender.setHttpClient(httpClient);
        return sender;
    }

}

