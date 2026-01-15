package org.example.adminPanelService.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.adminPanelService.dto.AuthResponse;
import org.example.adminPanelService.dto.LoginRequest;
import org.example.adminPanelService.dto.RegisterRequest;
import org.example.adminPanelService.security.JwtUtil;
import org.example.adminPanelService.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/auth")
public class AuthController {
    @Autowired private AuthService service;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        service.register(req);
        return ResponseEntity.ok(new AuthResponse("Registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletResponse res) {
        String token = service.authenticate(req, res);
        return ResponseEntity.ok(new AuthResponse(token));
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

