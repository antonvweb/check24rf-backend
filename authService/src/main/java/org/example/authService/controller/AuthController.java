package org.example.authService.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authService.dto.AuthResponse;
import org.example.authService.dto.LoginRequest;
import org.example.authService.dto.VerifyRequest;
import org.example.authService.security.JwtUtil;
import org.example.authService.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(".api/auth")
public class AuthController {
    @Autowired private AuthService service;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletResponse res) {
        String token = service.authenticate(req, res);
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

