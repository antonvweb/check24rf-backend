package org.example.authService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.dto.TelegramUpdate;
import org.example.authService.service.TelegramBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Контроллер для обработки webhook от Telegram Bot API
 */
@Slf4j
@RestController
@RequestMapping("/api/telegram")
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;
    private final ObjectMapper objectMapper;

    public TelegramWebhookController(
            TelegramBotService telegramBotService,
            ObjectMapper objectMapper) {
        this.telegramBotService = telegramBotService;
        this.objectMapper = objectMapper;
    }

    /**
     * Webhook для получения обновлений от Telegram
     * Telegram отправляет POST запросы на этот эндпоинт
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        log.debug("Получено обновление от Telegram: {}", payload);

        try {
            TelegramUpdate update = objectMapper.readValue(payload, TelegramUpdate.class);
            telegramBotService.handleUpdate(update);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Ошибка обработки обновления от Telegram", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Эндпоинт для проверки работоспособности
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Telegram webhook is running");
    }

    /**
     * Установить webhook (для ручного вызова)
     */
    @PostMapping("/set-webhook")
    public Mono<ResponseEntity<String>> setWebhook(@RequestParam String url) {
        log.info("Установка webhook на URL: {}", url);
        return telegramBotService.setWebhook(url)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Ошибка установки webhook", error);
                    return Mono.just(ResponseEntity.internalServerError().body(error.getMessage()));
                });
    }

    /**
     * Получить информацию о webhook
     */
    @GetMapping("/webhook-info")
    public Mono<ResponseEntity<String>> getWebhookInfo() {
        return telegramBotService.getWebhookInfo()
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(error -> {
                    log.error("Ошибка получения информации о webhook", error);
                    return Mono.just(ResponseEntity.internalServerError().body(error.getMessage()));
                });
    }
}
