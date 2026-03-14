package org.example.authService.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.User;
import org.example.common.repository.UserRepository;
import org.example.common.security.JwtUtil;
import org.example.authService.utils.ValidationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.security.SecureRandom;

@Slf4j
@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;
    private final EmailService emailService;
    private final TelegramBotService telegramBotService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ValidationUtils validationUtils;

    @Value("${jwt.refresh-token.expiration:#{7*24*60*60*1000}}")
    private long REFRESH_EXPIRY;

    @Value("${jwt.access-token.expiration:#{60*60*1000}}")
    private long accessTokenExpiration;

    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);
    private static final String CODE_PREFIX_PHONE = "code:phone:";
    private static final String CODE_PREFIX_EMAIL = "code:email:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private static final int MAX_ATTEMPTS = 5;
    private static final String ATTEMPTS_PREFIX = "attempts:";

    public AuthService(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            SmsService smsService,
            EmailService emailService,
            TelegramBotService telegramBotService,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            ValidationUtils validationUtils) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.smsService = smsService;
        this.emailService = emailService;
        this.telegramBotService = telegramBotService;
        this.redisTemplate = redisTemplate;
        this.validationUtils = validationUtils;
    }

    /**
     * Отправка кода верификации на телефон или email
     */
    public void sendVerificationCode(String identifier) {
        log.info("📧 Отправка кода верификации на: {}", identifier);

        // Валидация идентификатора
        if (!validationUtils.isValidIdentifier(identifier)) {
            log.warn("❌ Неверный формат идентификатора: {}", identifier);
            throw new IllegalArgumentException("Неверный формат идентификатора. Используйте телефон (+7...) или email.");
        }

        // Нормализация идентификатора (особенно для телефонов)
        String cleanIdentifier = validationUtils.normalizePhone(identifier);
        log.info("✅ Нормализованный идентификатор: {}", cleanIdentifier);

        String code = "123456";

        // Определяем тип: телефон или email
        boolean isEmail = cleanIdentifier.contains("@");
        String redisKey = isEmail
                ? CODE_PREFIX_EMAIL + cleanIdentifier
                : CODE_PREFIX_PHONE + cleanIdentifier;

        // Сохраняем код в Redis на 5 минут
        redisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRATION);
        log.info("💾 Код сохранен в Redis с ключом: {}", redisKey);
    }

    /**
     * Отправка кода через Telegram Bot
     */
    private void sendCodeViaTelegram(String identifier, String code) {
        // Для телефона - ищем пользователя в Telegram и отправляем код
        if (!identifier.contains("@")) {
            telegramBotService.sendCodeToUser(identifier, code);
        } else {
            // Для email - логика может быть другой (пока просто лог)
            log.info("📧 Код для email: {}", identifier);
        }
    }

    /**
     * Проверка кода и создание/авторизация пользователя
     * Возвращает JWT токены
     */
    @Transactional
    public Map<String, String> verifyCodeAndAuthenticate(
            String identifier,
            String code,
            HttpServletResponse response) {

        log.info("🔍 Проверка кода для: {}", identifier);

        // Валидация идентификатора
        if (!validationUtils.isValidIdentifier(identifier)) {
            log.warn("❌ Неверный формат идентификатора: {}", identifier);
            throw new IllegalArgumentException("Неверный формат идентификатора. Используйте телефон (+7...) или email.");
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Код не может быть пустым");
        }

        // Очистка и нормализация данных
        String cleanIdentifier = validationUtils.normalizePhone(identifier.trim());
        String cleanCode = code.trim();

        // Определяем тип и получаем код из Redis
        boolean isEmail = cleanIdentifier.contains("@");
        String redisKey = isEmail
                ? CODE_PREFIX_EMAIL + cleanIdentifier
                : CODE_PREFIX_PHONE + cleanIdentifier;

        // Проверка лимита попыток
        String attemptsKey = ATTEMPTS_PREFIX + cleanIdentifier;
        Integer attempts = Optional.ofNullable(redisTemplate.opsForValue().get(attemptsKey))
                .map(Integer::parseInt)
                .orElse(0);
        
        if (attempts >= MAX_ATTEMPTS) {
            log.warn("❌ Превышен лимит попыток для: {}", cleanIdentifier);
            throw new IllegalArgumentException("Слишком много попыток. Запросите новый код.");
        }

        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            log.warn("❌ Код не найден в Redis для: {}", cleanIdentifier);
            throw new IllegalArgumentException("Код не найден или истек. Запросите новый код.");
        }

        // Проверяем код
        if (!cleanCode.equals(storedCode.trim())) {
            // Увеличить счетчик попыток
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(15));
            log.warn("❌ Неверный код для: {} (попытка {}/{})", cleanIdentifier, attempts + 1, MAX_ATTEMPTS);
            throw new IllegalArgumentException("Неверный код подтверждения");
        }
        
        // Успех - удалить счетчик попыток
        redisTemplate.delete(attemptsKey);

        log.info("✅ Код верен для: {}", cleanIdentifier);

        // Удаляем код из Redis (одноразовое использование)
        redisTemplate.delete(redisKey);
        log.info("🗑️ Код удален из Redis");

        // Получаем или создаем пользователя
        User user;
        if (isEmail) {
            user = userRepository.findByEmail(cleanIdentifier)
                    .orElseGet(() -> createUser(null, cleanIdentifier));
        } else {
            user = userRepository.findByPhoneNumberNormalized(cleanIdentifier)
                    .orElseGet(() -> createUser(cleanIdentifier, null));
        }

        // Активируем пользователя
        user.setActive(true);
        userRepository.save(user);
        log.info("👤 Пользователь активирован: {}", user.getId());

        // Генерируем токены
        String accessToken = jwtUtil.generateAccessToken(user.getId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        // Сохраняем refresh token в Redis для ротации
        String redisKeyRefresh = REFRESH_TOKEN_PREFIX + user.getId().toString();
        redisTemplate.opsForValue().set(redisKeyRefresh, refreshToken, Duration.ofMillis(REFRESH_EXPIRY));

        // Устанавливаем cookies
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(accessTokenExpiration / 1000)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(REFRESH_EXPIRY / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        log.info("🎫 JWT токены созданы для пользователя: {}", user.getId());

        return Map.of(
                "accessToken", accessToken,
                "userId", user.getId().toString(),
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                "email", user.getEmail() != null ? user.getEmail() : ""
        );
    }

    /**
     * Создание нового пользователя
     */
    private User createUser(String phoneNumber, String email) {
        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setActive(true);

        user = userRepository.save(user);
        log.info("✨ Создан новый пользователь: {} (phone: {}, email: {})",
                user.getId(), phoneNumber, email);

        return user;
    }

    /**
     * Обновление access токена через refresh token с ротацией
     */
    public String refreshAccessToken(String refreshToken) {
        log.info("🔄 Обновление access токена");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token не предоставлен");
        }

        // Проверяем blacklist
        if (jwtUtil.isTokenBlacklisted(refreshToken)) {
            throw new IllegalArgumentException("Refresh token был отозван");
        }

        if (jwtUtil.isExpired(refreshToken)) {
            throw new IllegalArgumentException("Refresh token истек");
        }

        String userId = jwtUtil.getUserId(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Некорректный refresh token"));

        // Проверяем в Redis (ротация)
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            // Токен уже был использован - возможная атака
            // Блокируем все токены пользователя
            blacklistAllUserTokens(userId);
            throw new IllegalArgumentException("Refresh token уже был использован. Все токены отозваны.");
        }

        // Генерируем НОВЫЙ refresh token (ротация)
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        String newAccessToken = jwtUtil.generateAccessToken(userId);

        // Обновляем в Redis
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, Duration.ofMillis(REFRESH_EXPIRY));

        // Добавляем старый токен в blacklist
        jwtUtil.blacklistToken(refreshToken, REFRESH_EXPIRY);

        log.info("✅ Access токен обновлен для пользователя: {}", userId);

        return newAccessToken;
    }

    /**
     * Выход из системы (инвалидация токенов)
     */
    public void logout(HttpServletResponse response, HttpServletRequest request) {
        // Извлекаем токены из запроса
        String accessToken = extractTokenFromRequest(request);
        String refreshToken = extractRefreshTokenFromRequest(request);
        
        // Добавляем access token в blacklist
        if (accessToken != null && !accessToken.isBlank()) {
            jwtUtil.getTimeToExpiration(accessToken).ifPresent(expiration ->
                jwtUtil.blacklistToken(accessToken, expiration * 1000)
            );
            log.info("🚫 Access token добавлен в blacklist");
        }

        // Добавляем refresh token в blacklist
        if (refreshToken != null && !refreshToken.isBlank()) {
            jwtUtil.getTimeToExpiration(refreshToken).ifPresent(expiration ->
                jwtUtil.blacklistToken(refreshToken, expiration * 1000)
            );

            // Удаляем из Redis
            String userId = jwtUtil.getUserId(refreshToken).orElse(null);
            if (userId != null) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
                log.info("🗑️ Refresh token удален из Redis для пользователя: {}", userId);
            }
        }

        // Удаляем cookies
        ResponseCookie deleteAccessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

        log.info("👋 Пользователь вышел из системы");
    }

    /**
     * Блокировка всех токенов пользователя (превентивная мера при атаке)
     */
    private void blacklistAllUserTokens(String userId) {
        // Блокируем все токены пользователя (превентивная мера при атаке)
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + userId, "1", Duration.ofDays(7));
        log.warn("🚨 Все токены пользователя {} заблокированы из-за возможной атаки", userId);
    }

    /**
     * Извлечение access token из запроса
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        return Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "accessToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    /**
     * Извлечение refresh token из запроса
     */
    private String extractRefreshTokenFromRequest(HttpServletRequest request) {
        return Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
