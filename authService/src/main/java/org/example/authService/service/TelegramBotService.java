package org.example.authService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.dto.TelegramUpdate;
import org.example.authService.entity.TelegramUser;
import org.example.authService.repository.TelegramUserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с Telegram ботом
 */
@Slf4j
@Service
public class TelegramBotService {

    private final WebClient webClient;
    private final String botToken;
    private final TelegramUserRepository telegramUserRepository;
    private final ObjectMapper objectMapper;

    public TelegramBotService(
            WebClient webClient,
            @org.springframework.beans.factory.annotation.Value("${telegram.bot.token:}") String botToken,
            TelegramUserRepository telegramUserRepository,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.botToken = botToken;
        this.telegramUserRepository = telegramUserRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Обработка входящего обновления от Telegram
     */
    @Transactional
    public void handleUpdate(TelegramUpdate update) {
        if (update.getMessage() != null) {
            handleMessage(update.getMessage());
        } else if (update.getCallbackQuery() != null) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    /**
     * Обработка сообщения
     */
    @Transactional
    public void handleMessage(org.example.authService.dto.TelegramMessage message) {
        if (message == null || message.getFrom() == null || message.getChat() == null) {
            return;
        }

        Long chatId = message.getChat().getId();
        String text = message.getText();

        // Сохраняем или обновляем пользователя
        saveOrUpdateUser(message.getFrom(), chatId);

        // Обрабатываем команды
        if (text != null && text.startsWith("/")) {
            handleCommand(chatId, text);
        }
    }

    /**
     * Обработка callback query (нажатия на кнопки)
     */
    public void handleCallbackQuery(org.example.authService.dto.TelegramCallbackQuery callbackQuery) {
        if (callbackQuery == null || callbackQuery.getFrom() == null) {
            return;
        }

        log.info("Получен callback query от пользователя: {}", callbackQuery.getFrom().getId());
        // Можно добавить обработку кнопок
    }

    /**
     * Сохранение или обновление пользователя в БД
     */
    private void saveOrUpdateUser(org.example.authService.dto.TelegramUserDto userDto, Long chatId) {
        telegramUserRepository.findByChatId(chatId)
                .ifPresentOrElse(
                        existingUser -> {
                            existingUser.setUsername(userDto.getUsername());
                            existingUser.setFirstName(userDto.getFirstName());
                            existingUser.setLastName(userDto.getLastName());
                            existingUser.setIsActive(true);
                            telegramUserRepository.save(existingUser);
                            log.info("Обновлен пользователь Telegram: {}", chatId);
                        },
                        () -> {
                            TelegramUser newUser = new TelegramUser();
                            newUser.setChatId(chatId);
                            newUser.setUsername(userDto.getUsername());
                            newUser.setFirstName(userDto.getFirstName());
                            newUser.setLastName(userDto.getLastName());
                            newUser.setIsActive(true);
                            telegramUserRepository.save(newUser);
                            log.info("Зарегистрирован новый пользователь Telegram: {}", chatId);
                        }
                );
    }

    /**
     * Обработка команд бота
     */
    private void handleCommand(Long chatId, String command) {
        log.info("Получена команда: {} от пользователя: {}", command, chatId);

        switch (command.toLowerCase()) {
            case "/start":
                sendStartMessage(chatId);
                break;
            case "/help":
                sendHelpMessage(chatId);
                break;
            default:
                sendUnknownCommandMessage(chatId);
        }
    }

    /**
     * Отправка приветственного сообщения на /start
     */
    private void sendStartMessage(Long chatId) {
        String message = "👋 Привет! Я бот для авторизации в системе Чек24.\n\n" +
                "🔐 Теперь коды авторизации будут приходить сюда.\n\n" +
                "Команды:\n" +
                "/help - справка\n" +
                "/start - начать заново";

        sendMessage(chatId, message);
        log.info("Отправлено приветственное сообщение пользователю: {}", chatId);
    }

    /**
     * Отправка справки на /help
     */
    private void sendHelpMessage(Long chatId) {
        String message = "ℹ️ *Справка*\n\n" +
                "Я бот для авторизации в системе Чек24.\n" +
                "Когда вы запрашиваете код авторизации на сайте, " +
                "он автоматически приходит в этот чат.\n\n" +
                "*Команды:*\n" +
                "/start - приветственное сообщение\n" +
                "/help - эта справка";

        sendMessage(chatId, message);
    }

    /**
     * Отправка сообщения о неизвестной команде
     */
    private void sendUnknownCommandMessage(Long chatId) {
        sendMessage(chatId, "❌ Неизвестная команда. Используйте /help для просмотра доступных команд.");
    }

    /**
     * Отправить код подтверждения пользователю
     */
    public void sendVerificationCode(Long chatId, String code) {
        String message = "🔐 Ваш код подтверждения: *" + code + "*\n\n" +
                "_Не сообщайте код никому_";
        sendMessage(chatId, message);
    }

    /**
     * Отправить сообщение пользователю
     */
    public void sendMessage(Long chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            log.warn("Токен Telegram бота не настроен");
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> requestBody = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "Markdown"
            );

            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(response);
                                    if (jsonNode.has("ok") && jsonNode.get("ok").asBoolean()) {
                                        log.debug("Сообщение успешно отправлено в Telegram (chat_id: {})", chatId);
                                    } else {
                                        log.error("Ошибка отправки в Telegram: {}", response);
                                    }
                                } catch (Exception e) {
                                    log.error("Ошибка парсинга ответа Telegram", e);
                                }
                            },
                            error -> log.error("Ошибка отправки сообщения в Telegram", error)
                    );

        } catch (Exception e) {
            log.error("Ошибка отправки сообщения в Telegram для chat_id: {}", chatId, e);
        }
    }

    /**
     * Отправить сообщение всем активным пользователям
     */
    @Transactional(readOnly = true)
    public void broadcastMessage(String text) {
        List<TelegramUser> activeUsers = telegramUserRepository.findByIsActiveTrue();
        log.info("Рассылка сообщения {} пользователям", activeUsers.size());

        for (TelegramUser user : activeUsers) {
            sendMessage(user.getChatId(), text);
        }
    }

    /**
     * Отправить код всем пользователям (для тестирования)
     */
    public void broadcastCode(String code) {
        broadcastMessage("🔐 Тестовый код: *" + code + "*");
    }

    /**
     * Установить webhook для получения обновлений
     */
    public Mono<String> setWebhook(String webhookUrl) {
        String url = "https://api.telegram.org/bot" + botToken + "/setWebhook";

        Map<String, Object> requestBody = Map.of(
                "url", webhookUrl
        );

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Получить информацию о webhook
     */
    public Mono<String> getWebhookInfo() {
        String url = "https://api.telegram.org/bot" + botToken + "/getWebhookInfo";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }
}
