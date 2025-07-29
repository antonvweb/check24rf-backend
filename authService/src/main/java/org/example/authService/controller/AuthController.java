package org.example.authService.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.authService.dto.*;
import org.example.authService.security.JwtUtil;
import org.example.authService.service.AuthService;
import org.example.authService.service.SmartCaptchaService;
import org.example.authService.utils.IPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private AuthService service;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private IPUtils ipUtils;
    @Autowired private SmartCaptchaService captchaService;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/verify-captcha")
    public ResponseEntity<?> verifyCaptcha(@RequestBody @Valid CaptchaRequest req, HttpServletRequest request){
        log.info("Captcha token received: {}", req.getCaptchaToken());
        // СНАЧАЛА проверяем каптчу
        String userIP = ipUtils.getClientIP(request);  // Теперь правильно используем request
        boolean captchaValid = captchaService.validateCaptchaSync(
                req.getCaptchaToken(),
                userIP
        );

        if (!captchaValid) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Captcha validation failed"));
        }

        return ResponseEntity.ok(true);
    }

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody LoginRequest request) {
        service.sendVerificationCode(request.getPhoneNumber());
        return ResponseEntity.ok("Code sent");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verify(@RequestBody @Valid VerifyRequest request,
                                    HttpServletRequest httpRequest, HttpServletResponse response) {

        String userIP = ipUtils.getClientIP(httpRequest);
        log.info("🔍 Code verification request from IP: {}", userIP);
        log.info("🔍 Request body: phone='{}', code='{}'",
                request.getPhoneNumber(),
                request.getCode());

        try {
            boolean success = service.verifyCode(request.getPhoneNumber(), request.getCode());

            if (success) {
                log.info("✅ Code verification successful for phone: {}", request.getPhoneNumber());
                return ResponseEntity.ok(Map.of(
                        "message", "Access allowed"
                ));
            } else {
                log.warn("❌ Code verification failed for phone: {}", request.getPhoneNumber());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Invalid code",
                                "code", "INVALID_VERIFICATION_CODE"
                        ));
            }
        } catch (Exception e) {
            log.error("❌ Unexpected error during code verification", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "code", "VERIFICATION_ERROR"
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response){
        Map<String, String> authData = service.authenticate(request, response);

        return ResponseEntity.ok(authData);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        String newAccessToken  = service.refreshToken(req, res);
        return ResponseEntity.ok(new TokenResponse(newAccessToken));
    }

    @GetMapping("/validate")
    public ResponseEntity<Void> validate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.replace("Bearer ", "");

        if (jwtUtil.isAccessTokenValid(token)) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

