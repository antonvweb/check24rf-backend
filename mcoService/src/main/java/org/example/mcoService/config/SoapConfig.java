package org.example.mcoService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class SoapConfig {

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.example.mcoService.dto");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller)
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setMessageSender(httpComponentsMessageSender());

        return template;
    }

    @Bean
    public HttpComponentsMessageSender httpComponentsMessageSender()
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        messageSender.setHttpClient(httpClient());

        return messageSender;
    }

    private CloseableHttpClient httpClient()
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        // ⚠️ Только для теста — доверяет всем сертификатам
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                (hostname, session) -> true
        );

        return HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .setSSLSocketFactory(socketFactory)
                .build();
    }
}
