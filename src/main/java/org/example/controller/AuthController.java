package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.AuthResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired private AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        service.register(req);
        return ResponseEntity.ok(new AuthResponse("Registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletResponse res) {
        service.authenticate(req, res);
        return ResponseEntity.ok(new AuthResponse("Login success"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        service.refreshToken(req, res);
        return ResponseEntity.ok(new AuthResponse("Token refreshed"));
    }
}

