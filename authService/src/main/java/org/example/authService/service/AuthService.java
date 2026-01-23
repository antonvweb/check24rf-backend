package org.example.authService.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.User;
import org.example.common.repository.UserRepository;
import org.example.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token.expiration:#{7*24*60*60*1000}}")
    private long REFRESH_EXPIRY;

    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);
    private static final String CODE_PREFIX_PHONE = "code:phone:";
    private static final String CODE_PREFIX_EMAIL = "code:email:";

    public AuthService(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            SmsService smsService,
            EmailService emailService,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.smsService = smsService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Отправка кода верификации на телефон или email
     */
    public void sendVerificationCode(String identifier) {
        log.info("📧 Отправка кода верификации на: {}", identifier);

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Идентификатор не может быть пустым");
        }

        String code = String.format("%06d", new Random().nextInt(1_000_000));

        // Определяем тип: телефон или email
        boolean isEmail = identifier.contains("@");
        String redisKey = isEmail
                ? CODE_PREFIX_EMAIL + identifier
                : CODE_PREFIX_PHONE + identifier;

        // Сохраняем код в Redis на 5 минут
        redisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRATION);
        log.info("💾 Код сохранен в Redis с ключом: {}", redisKey);

        // Отправляем код
        if (isEmail) {
            emailService.sendVerificationCode(identifier, code);
            log.info("✉️ Код отправлен на email: {}", identifier);
        } else {
            smsService.sendSms(identifier, "Ваш код подтверждения: " + code);
            log.info("📱 Код отправлен на телефон: {}", identifier);
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

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Идентификатор не может быть пустым");
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Код не может быть пустым");
        }

        // Очистка данных
        String cleanIdentifier = identifier.trim();
        String cleanCode = code.trim();

        // Определяем тип и получаем код из Redis
        boolean isEmail = cleanIdentifier.contains("@");
        String redisKey = isEmail
                ? CODE_PREFIX_EMAIL + cleanIdentifier
                : CODE_PREFIX_PHONE + cleanIdentifier;

        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            log.warn("❌ Код не найден в Redis для: {}", cleanIdentifier);
            throw new IllegalArgumentException("Код не найден или истек. Запросите новый код.");
        }

        // Проверяем код
        if (!cleanCode.equals(storedCode.trim())) {
            log.warn("❌ Неверный код для: {}", cleanIdentifier);
            throw new IllegalArgumentException("Неверный код подтверждения");
        }

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
            user = userRepository.findByPhoneNumber(cleanIdentifier)
                    .orElseGet(() -> createUser(cleanIdentifier, null));
        }

        // Активируем пользователя
        user.setActive(true);
        userRepository.save(user);
        log.info("👤 Пользователь активирован: {}", user.getId());

        // Генерируем токены
        String accessToken = jwtUtil.generateAccessToken(user.getId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        // Устанавливаем refresh token в httpOnly cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true) // HTTPS only в production
                .path("/")
                .sameSite("Lax")
                .maxAge(REFRESH_EXPIRY / 1000)
                .build();

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
     * Обновление access токена через refresh token
     */
    public String refreshAccessToken(String refreshToken) {
        log.info("🔄 Обновление access токена");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token не предоставлен");
        }

        if (jwtUtil.isExpired(refreshToken)) {
            throw new IllegalArgumentException("Refresh token истек");
        }

        String userId = jwtUtil.getUserId(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Некорректный refresh token"));

        // Проверяем существование пользователя
        if (!userRepository.existsById(java.util.UUID.fromString(userId))) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId);
        log.info("✅ Access токен обновлен для пользователя: {}", userId);

        return newAccessToken;
    }

    /**
     * Выход из системы (инвалидация токенов)
     */
    public void logout(HttpServletResponse response) {
        // Удаляем refresh token cookie
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        log.info("👋 Пользователь вышел из системы");
    }
}