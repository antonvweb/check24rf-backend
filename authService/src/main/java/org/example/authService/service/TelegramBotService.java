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
import java.util.Optional;

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

        // Проверяем, ждём ли мы номер телефона от этого пользователя
        TelegramUser user = telegramUserRepository.findByChatId(chatId).orElse(null);
        
        if (user != null && user.getPhoneNumber() == null && text != null) {
            // Пользователь ещё не ввёл номер телефона - пытаемся сохранить
            handlePhoneNumberInput(chatId, text, user);
            return;
        }

        // Обрабатываем команды
        if (text != null && text.startsWith("/")) {
            handleCommand(chatId, text);
        }
    }

    /**
     * Обработка ввода номера телефона
     */
    @Transactional
    private void handlePhoneNumberInput(Long chatId, String phoneNumber, TelegramUser user) {
        log.info("Получен номер телефона от пользователя {}: {}", chatId, phoneNumber);

        // Очищаем номер от лишних символов
        String cleanPhone = phoneNumber.trim();
        
        // Проверяем формат номера
        if (!cleanPhone.startsWith("+")) {
            sendMessage(chatId, "❌ Номер телефона должен начинаться с + (например, +79051234567).\n\nПожалуйста, введите номер ещё раз:");
            return;
        }

        // Проверяем, не занят ли номер другим пользователем
        Optional<TelegramUser> existingUser = telegramUserRepository.findByPhoneNumber(cleanPhone);
        if (existingUser.isPresent() && !existingUser.get().getChatId().equals(chatId)) {
            sendMessage(chatId, "❌ Этот номер телефона уже привязан к другому пользователю.\n\nВведите другой номер или используйте команду /start заново:");
            return;
        }

        // Сохраняем номер телефона
        user.setPhoneNumber(cleanPhone);
        telegramUserRepository.save(user);

        sendMessage(chatId, "✅ Номер телефона *" + cleanPhone + "* успешно привязан!\n\n" +
                "Теперь коды авторизации будут приходить в этот чат.\n\n" +
                "Команды:\n" +
                "/help - справка\n" +
                "/start - начать заново");
        
        log.info("Номер телефона {} привязан к chat_id: {}", cleanPhone, chatId);
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
                            log.debug("Обновлен пользователь Telegram: {}", chatId);
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
        // Проверяем, есть ли уже пользователь
        TelegramUser user = telegramUserRepository.findByChatId(chatId).orElse(null);
        
        if (user != null && user.getPhoneNumber() != null) {
            // Пользователь уже привязал номер
            String message = "👋 Привет! Вы уже авторизованы в системе Чек24.\n\n" +
                    "Ваш номер телефона: *" + user.getPhoneNumber() + "*\n\n" +
                    "Коды авторизации приходят в этот чат.\n\n" +
                    "Команды:\n" +
                    "/help - справка\n" +
                    "/start - начать заново";
            sendMessage(chatId, message);
        } else {
            // Новый пользователь или не ввёл номер
            String message = "👋 Привет! Я бот для авторизации в системе Чек24.\n\n" +
                    "🔐 Для получения кодов авторизации необходимо привязать номер телефона.\n\n" +
                    "📱 *Пожалуйста, отправьте ваш номер телефона в формате:*\n" +
                    "_+79051234567_\n\n" +
                    "(начинается с плюса, без пробелов и скобок)";
            sendMessage(chatId, message);
        }
    }

    /**
     * Отправка справки на /help
     */
    private void sendHelpMessage(Long chatId) {
        TelegramUser user = telegramUserRepository.findByChatId(chatId).orElse(null);
        
        String message;
        if (user == null || user.getPhoneNumber() == null) {
            message = "ℹ️ *Справка*\n\n" +
                    "Я бот для авторизации в системе Чек24.\n" +
                    "Когда вы запрашиваете код авторизации на сайте, " +
                    "он автоматически приходит в этот чат.\n\n" +
                    "⚠️ *Вы ещё не привязали номер телефона!*\n\n" +
                    "📱 Отправьте ваш номер телефона в формате:\n" +
                    "_+79051234567_\n\n" +
                    "*Команды:*\n" +
                    "/start - начать привязку номера";
        } else {
            message = "ℹ️ *Справка*\n\n" +
                    "Я бот для авторизации в системе Чек24.\n" +
                    "Когда вы запрашиваете код авторизации на сайте, " +
                    "он автоматически приходит в этот чат.\n\n" +
                    "✅ Ваш номер телефона: *" + user.getPhoneNumber() + "*\n\n" +
                    "*Команды:*\n" +
                    "/start - начать заново";
        }

        sendMessage(chatId, message);
    }

    /**
     * Отправка сообщения о неизвестной команде
     */
    private void sendUnknownCommandMessage(Long chatId) {
        sendMessage(chatId, "❌ Неизвестная команда. Используйте /help для просмотра доступных команд.");
    }

    /**
     * Отправить код подтверждения пользователю по chat_id
     */
    public void sendVerificationCode(Long chatId, String code) {
        String message = "🔐 Ваш код подтверждения: *" + code + "*\n\n" +
                "_Не сообщайте код никому_";
        sendMessage(chatId, message);
    }

    /**
     * Отправить код подтверждения пользователю по номеру телефона
     */
    @Transactional(readOnly = true)
    public void sendVerificationCodeByPhone(String phoneNumber) {
        telegramUserRepository.findByPhoneNumber(phoneNumber)
                .ifPresentOrElse(
                        user -> {
                            // Код будет отправлен из AuthService
                            log.info("Найден пользователь для номера {}: chat_id={}", phoneNumber, user.getChatId());
                        },
                        () -> log.warn("Пользователь с номером {} не найден в Telegram", phoneNumber)
                );
    }

    /**
     * Получить chat_id по номеру телефона
     */
    @Transactional(readOnly = true)
    public Optional<Long> getChatIdByPhoneNumber(String phoneNumber) {
        return telegramUserRepository.findByPhoneNumber(phoneNumber)
                .map(TelegramUser::getChatId);
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
