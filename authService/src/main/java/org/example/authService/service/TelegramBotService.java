package org.example.authService.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authService.entity.TelegramUser;
import org.example.authService.repository.TelegramUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Optional;

/**
 * Telegram бот на базе библиотеки TelegramBots
 * Использует long polling — библиотека сама опрашивает Telegram
 */
@Slf4j
@Service
public class TelegramBotService extends TelegramLongPollingBot implements CommandLineRunner {

    private final TelegramUserRepository telegramUserRepository;
    private final String botToken;

    public TelegramBotService(
            TelegramUserRepository telegramUserRepository,
            @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.telegramUserRepository = telegramUserRepository;
        this.botToken = botToken;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("🤖 Telegram бот запускается...");

        // Регистрируем бота
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
        
        log.info("✅ Бот зарегистрирован и готов к работе");
    }

    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    @Transactional
    public void handleMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String username = message.getFrom().getUserName();
        String firstName = message.getFrom().getFirstName();
        String text = message.getText();

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
            sendMessage(chatId, "Привет!\n\n" +
                    "Ваш номер: *" + user.getPhoneNumber() + "*\n\n" +
                    "Коды авторизации приходят в этот чат.");
        } else {
            sendMessage(chatId, "Привет! Я бот для авторизации в Чек24.\n\n" +
                    "*Отправьте ваш номер телефона:*\n" +
                    "_+79051234567_\n\n" +
                    "(с плюсом, без пробелов и скобок)");
        }
    }

    private void handleHelp(Long chatId, TelegramUser user) {
        if (user.getPhoneNumber() == null) {
            sendMessage(chatId, "ℹ*Справка*\n\n" +
                    "Отправьте номер телефона в формате:\n" +
                    "_+79051234567_\n\n" +
                    "После этого коды авторизации будут приходить в этот чат.");
        } else {
            sendMessage(chatId, "ℹ*Справка*\n\n" +
                    "Ваш номер: *" + user.getPhoneNumber() + "*\n\n" +
                    "Когда запрашиваете код на сайте - он приходит в этот чат.\n\n" +
                    "/start - начать заново");
        }
    }

    private void handlePhoneInput(Long chatId, TelegramUser user, String phone) {
        String cleanPhone = phone.trim();

        if (!cleanPhone.startsWith("+")) {
            sendMessage(chatId, "Номер должен начинаться с +\n\n" +
                    "Пример: _+79051234567_\n\n" +
                    "Введите ещё раз:");
            return;
        }

        // Проверяем что номер содержит только цифры после + (от 10 до 15 цифр)
        if (!cleanPhone.matches("\\+\\d{10,15}")) {
            sendMessage(chatId, "Неверный формат номера.\n\n" +
                    "Номер должен содержать от 10 до 15 цифр после +\n" +
                    "Пример: _+79051234567_\n\n" +
                    "Введите ещё раз:");
            return;
        }

        // Проверяем, не занят ли номер другим пользователем
        Optional<TelegramUser> existing = telegramUserRepository.findByPhoneNumber(cleanPhone);
        if (existing.isPresent() && !existing.get().getChatId().equals(chatId)) {
            sendMessage(chatId, "Этот номер уже привязан к другому пользователю.\n\n" +
                    "Введите другой номер или используйте /start:");
            return;
        }

        user.setPhoneNumber(cleanPhone);
        telegramUserRepository.save(user);

        sendMessage(chatId, "Номер *" + cleanPhone + "* привязан!\n\n" +
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
                                "Ваш код подтверждения: *" + code);
                            log.info("✉️ Код отправлен в Telegram для {}: chat_id={}", phone, user.getChatId());
                        },
                        () -> log.warn("⚠️ Пользователь с номером {} не найден в Telegram", phone)
                );
    }

    /**
     * Отправить сообщение пользователю
     */
    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);

        try {
            execute(message);
            log.debug("✅ Сообщение отправлено в {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения в {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "Check24Bot"; // любое имя
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
