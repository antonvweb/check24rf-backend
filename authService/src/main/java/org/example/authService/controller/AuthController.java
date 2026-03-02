package org.example.authService.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.dto.*;
import org.example.authService.service.AuthService;
import org.example.authService.service.SmartCaptchaService;
import org.example.authService.utils.IPUtils;
import org.example.common.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер авторизации с правильной логикой
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final IPUtils ipUtils;
    private final SmartCaptchaService captchaService;

    /**
     * Шаг 1: Отправка кода верификации
     * POST /api/auth/send-code
     *
     * Body: { "identifier": "79054455906" } или { "identifier": "user@example.com" }
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(
            @Valid @RequestBody SendCodeRequest request) {

        try {
            log.info("📧 Запрос отправки кода на: {}", request.getIdentifier());
            authService.sendVerificationCode(request.getIdentifier());

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Код отправлен")
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Ошибка отправки кода: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при отправке кода", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Внутренняя ошибка сервера")
                            .build());
        }
    }

    /**
     * Шаг 2: Проверка кода + капчи + авторизация
     * POST /api/auth/verify
     *
     * Body: {
     *   "identifier": "79054455906",
     *   "code": "123456",
     *   "captchaToken": "..."
     * }
     *
     * Response: {
     *   "success": true,
     *   "data": {
     *     "userId": "...",
     *     "phoneNumber": "...",
     *     "email": "..."
     *   }
     * }
     * 
     * Токены устанавливаются в httpOnly cookies
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verify(
            @Valid @RequestBody VerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String userIP = ipUtils.getClientIP(httpRequest);
        log.info("🔍 Проверка кода от IP: {}", userIP);
        log.info("🔍 Идентификатор: {}, Код: {}",
                request.getIdentifier(), request.getCode());

        try {
            // 2. Проверяем код и создаем/авторизуем пользователя
            Map<String, String> authData = authService.verifyCodeAndAuthenticate(
                    request.getIdentifier(),
                    request.getCode(),
                    httpResponse
            );

            AuthResponse response = AuthResponse.builder()
                    .userId(authData.get("userId"))
                    .phoneNumber(authData.get("phoneNumber"))
                    .email(authData.get("email"))
                    .build();

            log.info("✅ Пользователь успешно авторизован: {}", response.getUserId());

            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .success(true)
                    .message("Авторизация успешна")
                    .data(response)
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Ошибка верификации: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при верификации", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<AuthResponse>builder()
                            .success(false)
                            .message("Внутренняя ошибка сервера")
                            .build());
        }
    }

    /**
     * Проверка валидности капчи (опционально, для отладки)
     * POST /api/auth/verify-captcha
     */
    @PostMapping("/verify-captcha")
    public ResponseEntity<ApiResponse<Boolean>> verifyCaptcha(
            @Valid @RequestBody CaptchaRequest request,
            HttpServletRequest httpRequest) {

        try {
            String userIP = ipUtils.getClientIP(httpRequest);
            log.info("🤖 Проверка капчи от IP: {}", userIP);

            boolean isValid = captchaService.validateCaptchaSync(
                    request.getCaptchaToken(),
                    userIP
            );

            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .success(isValid)
                    .message(isValid ? "Капча валидна" : "Капча не валидна")
                    .data(isValid)
                    .build());

        } catch (Exception e) {
            log.error("❌ Ошибка проверки капчи", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Boolean>builder()
                            .success(false)
                            .message("Ошибка проверки капчи")
                            .build());
        }
    }

    /**
     * Обновление access токена через refresh token с ротацией
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<TokenResponse>builder()
                                .success(false)
                                .message("Refresh token не предоставлен")
                                .build());
            }

            String newAccessToken = authService.refreshAccessToken(refreshToken);

            // Устанавливаем новый access token в cookie
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(3600)
                    .build();

            response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .build();

            return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                    .success(true)
                    .message("Токен обновлен")
                    .data(tokenResponse)
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Ошибка обновления токена: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<TokenResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при обновлении токена", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<TokenResponse>builder()
                            .success(false)
                            .message("Внутренняя ошибка сервера")
                            .build());
        }
    }

    /**
     * Проверка валидности access токена
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String token = null;
        
        // Пробуем получить из Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.replace("Bearer ", "");
        } else {
            // Пытаемся получить из cookie
            token = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                    .filter(c -> "accessToken".equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Boolean>builder()
                            .success(false)
                            .message("Токен не предоставлен")
                            .data(false)
                            .build());
        }

        boolean isValid = jwtUtil.isAccessTokenValid(token);

        if (isValid) {
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .success(true)
                    .message("Токен валиден")
                    .data(true)
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Boolean>builder()
                            .success(false)
                            .message("Токен не валиден")
                            .data(false)
                            .build());
        }
    }

    /**
     * Выход из системы (инвалидация токенов)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            String accessToken = null;
            
            // Пробуем получить из Authorization header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            } else {
                // Пытаемся получить из cookie
                accessToken = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                        .filter(c -> "accessToken".equals(c.getName()))
                        .findFirst()
                        .map(Cookie::getValue)
                        .orElse(null);
            }

            authService.logout(response, accessToken);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Выход выполнен успешно")
                    .build());

        } catch (Exception e) {
            log.error("❌ Ошибка при выходе", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Ошибка при выходе")
                            .build());
        }
    }

    /**
     * Получение CSRF токена
     * GET /api/auth/csrf-token
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<ApiResponse<CsrfTokenResponse>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        CsrfTokenResponse response = new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
        return ResponseEntity.ok(ApiResponse.<CsrfTokenResponse>builder()
                .success(true)
                .data(response)
                .build());
    }
}
