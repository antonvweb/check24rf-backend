package org.example.authService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.entity.TelegramUser;
import org.example.authService.repository.TelegramUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Telegram бот с long polling
 * Работает внутри authService, опрашивает Telegram API
 */
@Slf4j
@Service
public class TelegramBotService implements CommandLineRunner {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final TelegramUserRepository telegramUserRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TelegramBotService(TelegramUserRepository telegramUserRepository, ObjectMapper objectMapper) {
        this.telegramUserRepository = telegramUserRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🤖 Telegram бот запускается...");
        
        // Удаляем webhook если был
        deleteWebhook();
        
        // Запускаем long polling в отдельном потоке
        new Thread(this::startPolling).start();
    }

    private void deleteWebhook() {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/deleteWebhook";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("✅ Webhook удален: {}", response.body());
        } catch (Exception e) {
            log.warn("⚠️ Не удалось удалить webhook: {}", e.getMessage());
        }
    }

    private void startPolling() {
        log.info("🔄 Long polling запущен...");
        Integer offset = null;

        while (true) {
            try {
                String url = "https://api.telegram.org/bot" + botToken + "/getUpdates";
                if (offset != null) {
                    url += "?offset=" + (offset + 1);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode jsonNode = objectMapper.readTree(response.body());

                if (jsonNode.has("result")) {
                    for (JsonNode update : jsonNode.get("result")) {
                        offset = update.get("update_id").asInt();
                        handleUpdate(update);
                    }
                }
            } catch (Exception e) {
                log.error("❌ Ошибка polling: {}", e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ie) {}
            }
        }
    }

    @Transactional
    public void handleUpdate(JsonNode update) {
        if (update.has("message")) {
            handleMessage(update.get("message"));
        }
    }

    @Transactional
    public void handleMessage(JsonNode message) {
        if (!message.has("from") || !message.has("chat")) return;

        Long chatId = message.get("chat").get("id").asLong();
        Long userId = message.get("from").get("id").asLong();
        String username = message.get("from").has("username")
                ? message.get("from").get("username").asText() : null;
        String firstName = message.get("from").has("first_name")
                ? message.get("from").get("first_name").asText() : null;
        String text = message.has("text") ? message.get("text").asText() : null;

        log.info("📨 Сообщение от {}: {}", chatId, text);

        // Сохраняем пользователя
        TelegramUser user = telegramUserRepository.findByChatId(chatId).orElse(null);
        
        if (user == null) {
            user = new TelegramUser();
            user.setChatId(chatId);
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setIsActive(true);
            telegramUserRepository.save(user);
            log.info("✨ Новый пользователь: {}", chatId);
        } else {
            // Обновляем данные
            user.setUsername(username);
            user.setFirstName(firstName);
            telegramUserRepository.save(user);
        }

        // Обрабатываем команды
        if (text != null) {
            if ("/start".equals(text)) {
                handleStart(chatId, user);
                return;
            }
            if ("/help".equals(text)) {
                handleHelp(chatId, user);
                return;
            }

            // Если номер не привязан - сохраняем как номер телефона
            if (user.getPhoneNumber() == null && !text.startsWith("/")) {
                handlePhoneInput(chatId, user, text);
                return;
            }
        }
    }

    private void handleStart(Long chatId, TelegramUser user) {
        if (user.getPhoneNumber() != null) {
            sendMessage(chatId, "👋 Привет!\n\n" +
                    "Ваш номер: *" + user.getPhoneNumber() + "*\n\n" +
                    "Коды авторизации приходят в этот чат.\n\n" +
                    "/help - справка");
        } else {
            sendMessage(chatId, "👋 Привет! Я бот для авторизации в Чек24.\n\n" +
                    "📱 *Отправьте ваш номер телефона:*\n" +
                    "_+79051234567_\n\n" +
                    "(с плюсом, без пробелов и скобок)");
        }
    }

    private void handleHelp(Long chatId, TelegramUser user) {
        if (user.getPhoneNumber() == null) {
            sendMessage(chatId, "ℹ️ *Справка*\n\n" +
                    "Отправьте номер телефона в формате:\n" +
                    "_+79051234567_\n\n" +
                    "После этого коды авторизации будут приходить в этот чат.");
        } else {
            sendMessage(chatId, "ℹ️ *Справка*\n\n" +
                    "Ваш номер: *" + user.getPhoneNumber() + "*\n\n" +
                    "Когда запрашиваете код на сайте - он приходит в этот чат.\n\n" +
                    "/start - начать заново");
        }
    }

    private void handlePhoneInput(Long chatId, TelegramUser user, String phone) {
        String cleanPhone = phone.trim();
        
        if (!cleanPhone.startsWith("+")) {
            sendMessage(chatId, "❌ Номер должен начинаться с +\n\n" +
                    "Пример: _+79051234567_\n\n" +
                    "Введите ещё раз:");
            return;
        }

        // Проверяем, не занят ли номер другим пользователем
        Optional<TelegramUser> existing = telegramUserRepository.findByPhoneNumber(cleanPhone);
        if (existing.isPresent() && !existing.get().getChatId().equals(chatId)) {
            sendMessage(chatId, "❌ Этот номер уже привязан к другому пользователю.\n\n" +
                    "Введите другой номер или используйте /start:");
            return;
        }

        user.setPhoneNumber(cleanPhone);
        telegramUserRepository.save(user);
        
        sendMessage(chatId, "✅ Номер *" + cleanPhone + "* привязан!\n\n" +
                "Теперь коды авторизации будут приходить в этот чат.\n\n" +
                "/help - справка");
        
        log.info("✅ Номер {} привязан к chat_id: {}", cleanPhone, chatId);
    }

    /**
     * Отправить код подтверждения пользователю по номеру телефона
     */
    @Transactional(readOnly = true)
    public void sendCodeToUser(String phone, String code) {
        telegramUserRepository.findByPhoneNumber(phone)
                .ifPresentOrElse(
                        user -> {
                            sendMessage(user.getChatId(), 
                                "🔐 Ваш код подтверждения: *" + code + "*\n\n" +
                                "_Не сообщайте код никому_");
                            log.info("✉️ Код отправлен в Telegram для {}: chat_id={}", phone, user.getChatId());
                        },
                        () -> log.warn("⚠️ Пользователь с номером {} не найден в Telegram", phone)
                );
    }

    /**
     * Отправить сообщение пользователю
     */
    public void sendMessage(Long chatId, String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            
            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "Markdown"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .header("Content-Type", "application/json")
                    .build();

            // Асинхронная отправка
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            JsonNode resp = objectMapper.readTree(response.body());
                            if (!resp.has("ok") || !resp.get("ok").asBoolean()) {
                                log.error("❌ Ошибка отправки в Telegram: {}", response.body());
                            }
                        } catch (Exception e) {
                            log.error("❌ Ошибка парсинга ответа: {}", e.getMessage());
                        }
                        return response;
                    });
        } catch (Exception e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    /**
     * Получить chat_id по номеру телефона
     */
    @Transactional(readOnly = true)
    public Long getChatIdByPhone(String phone) {
        return telegramUserRepository.findByPhoneNumber(phone)
                .map(TelegramUser::getChatId)
                .orElse(null);
    }
}
