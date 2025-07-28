package org.example.authService.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.dto.SmartCaptchaResponse;
import org.example.authService.entity.SmartCaptchaProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmartCaptchaService {

    @Autowired private SmartCaptchaProperties captchaProperties;
    @Autowired private WebClient webClient;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    public Mono<Boolean> validateCaptcha(String token, String userIP) {
        if (StringUtils.isBlank(token)) {
            return Mono.just(false);
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("smartcaptcha.yandexcloud.net")
                        .path("/validate")
                        .queryParam("secret", captchaProperties.getServerKey())
                        .queryParam("token", token)
                        .queryParam("ip", userIP)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    return Mono.error(new RuntimeException("Captcha validation error"));
                })
                .bodyToMono(SmartCaptchaResponse.class)
                .map(SmartCaptchaResponse::isValid);
    }

    // Синхронная версия для блокирующего кода
    public boolean validateCaptchaSync(String token, String userIP) {
        try {
            return Boolean.TRUE.equals(validateCaptcha(token, userIP).block(Duration.ofSeconds(5)));
        } catch (Exception e) {
            return true; // Разрешаем доступ при ошибке
        }
    }
}