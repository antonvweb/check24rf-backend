package org.example.authService.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authService.dto.*;
import org.example.authService.security.JwtUtil;
import org.example.authService.service.AuthService;
import org.example.authService.service.SmartCaptchaService;
import org.example.authService.utils.IPUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     *     "accessToken": "...",
     *     "userId": "...",
     *     "phoneNumber": "...",
     *     "email": "..."
     *   }
     * }
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
            // 1. Проверяем капчу
            boolean captchaValid = captchaService.validateCaptchaSync(
                    request.getCaptchaToken(),
                    userIP
            );
            
            if (!captchaValid) {
                log.warn("❌ Капча не прошла проверку для IP: {}", userIP);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<AuthResponse>builder()
                                .success(false)
                                .message("Капча не прошла проверку")
                                .build());
            }
            
            log.info("✅ Капча прошла проверку");
            
            // 2. Проверяем код и создаем/авторизуем пользователя
            Map<String, String> authData = authService.verifyCodeAndAuthenticate(
                    request.getIdentifier(),
                    request.getCode(),
                    httpResponse
            );
            
            AuthResponse response = AuthResponse.builder()
                    .accessToken(authData.get("accessToken"))
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
     * Обновление access токена через refresh token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<TokenResponse>builder()
                                .success(false)
                                .message("Refresh token не предоставлен")
                                .build());
            }
            
            String newAccessToken = authService.refreshAccessToken(refreshToken);
            
            TokenResponse response = TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                    .success(true)
                    .message("Токен обновлен")
                    .data(response)
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
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Boolean>builder()
                            .success(false)
                            .message("Токен не предоставлен")
                            .data(false)
                            .build());
        }
        
        String token = authHeader.replace("Bearer ", "");
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
     * Выход из системы
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        try {
            authService.logout(response);
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
}
