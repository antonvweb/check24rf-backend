package org.example.authService.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.authService.dto.AuthResponse;
import org.example.authService.dto.LoginRequest;
import org.example.authService.dto.VerifyRequest;
import org.example.authService.security.JwtUtil;
import org.example.authService.service.AuthService;
import org.example.authService.service.SmartCaptchaService;
import org.example.authService.utils.IPUtils;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req,
                                   HttpServletRequest request,  // Изменено с HttpServletResponse
                                   HttpServletResponse response) {

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

        // ПОТОМ аутентифицируем пользователя
        String token = service.authenticate(req, response);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@RequestBody LoginRequest request) {
        service.sendVerificationCode(request.getPhoneNumber());
        return ResponseEntity.ok("Code sent");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verify(@RequestBody VerifyRequest request) {
        boolean success = service.verifyCode(request.getPhoneNumber(), request.getCode());
        return success
                ? ResponseEntity.ok("Access allowed")
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid code");
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        String newAccessToken  = service.refreshToken(req, res);
        return ResponseEntity.ok(new AuthResponse(newAccessToken ));
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

