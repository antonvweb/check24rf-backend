package org.example.authService.service;

import io.micrometer.common.util.StringUtils;
import org.example.authService.dto.CaptchaResponse;
import org.example.authService.entity.SmartCaptchaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class SmartCaptchaService {

    private static final Logger log = LoggerFactory.getLogger(SmartCaptchaService.class);

    private final SmartCaptchaProperties captchaProperties;
    private final WebClient webClient;

    @Autowired
    public SmartCaptchaService(SmartCaptchaProperties captchaProperties, WebClient webClient) {
        this.captchaProperties = captchaProperties;
        this.webClient = webClient;
    }

    public Mono<Boolean> validateCaptcha(String token, String userIP) {
        if (StringUtils.isBlank(token)) {
            log.warn("Captcha token is blank");
            return Mono.just(false);
        }

        String validateUrl = captchaProperties.getValidateUrl();
        if (StringUtils.isBlank(validateUrl)) {
            validateUrl = "https://smartcaptcha.yandexcloud.net/validate";
        }

        return webClient.get()
                .uri(validateUrl + "?secret={secret}&token={token}&ip={ip}",
                        captchaProperties.getServerKey(), token, userIP)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("SmartCaptcha validation failed with status: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Captcha validation error"));
                })
                .bodyToMono(CaptchaResponse.class)
                .doOnNext(response -> log.info("SmartCaptcha response: {}", response))
                .map(CaptchaResponse::isValid)
                .doOnError(e -> log.error("Error during captcha validation", e))
                .onErrorReturn(false);
    }

    public boolean validateCaptchaSync(String token, String userIP) {
        try {
            Boolean result = validateCaptcha(token, userIP)
                    .defaultIfEmpty(false)
                    .block(Duration.ofSeconds(5));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Captcha validation timeout or error", e);
            return false;
        }
    }
}