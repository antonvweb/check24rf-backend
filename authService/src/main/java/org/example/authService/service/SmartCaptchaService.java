package org.example.authService.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.controller.AuthController;
import org.example.authService.dto.SmartCaptchaResponse;
import org.example.authService.entity.SmartCaptchaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmartCaptchaService {

    @Autowired private SmartCaptchaProperties captchaProperties;
    @Autowired private WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(SmartCaptchaService.class);

    public Mono<Boolean> validateCaptcha(String token, String userIP) {
        if (StringUtils.isBlank(token)) {
            return Mono.just(false);
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("smartcaptcha.yandexcloud.net")
                        .port(443)
                        .path("/validate")
                        .queryParam("secret", captchaProperties.getServerKey())
                        .queryParam("token", token)
                        .queryParam("ip", userIP)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("SmartCaptcha validation failed with status: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Captcha validation error"));
                })
                .bodyToMono(SmartCaptchaResponse.class)
                .doOnNext(response -> log.info("SmartCaptcha response: {}", response))
                .map(SmartCaptchaResponse::isValid)
                .doOnSuccess(result -> log.debug("Captcha validation result: {}", result))
                .doOnError(e -> log.error("Error during captcha validation", e))
                .onErrorReturn(true);  // На ваше усмотрение, можно и false вернуть
    }


    // Синхронная версия для блокирующего кода
    public boolean validateCaptchaSync(String token, String userIP) {
        try {
            return Boolean.TRUE.equals(validateCaptcha(token, userIP).block(Duration.ofSeconds(5)));
        } catch (Exception e) {
            log.error("Captcha validation timeout or error", e);
            return true; // Разрешаем доступ при ошибке
        }
    }
}