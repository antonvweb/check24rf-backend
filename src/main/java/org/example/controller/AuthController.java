package org.example.controller;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegisterRequest;
import org.example.dto.TokenRequest;
import org.example.entity.Role;
import org.example.security.JwtUtil;
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
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String token = authService.login(request.getLogin(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        String token = authService.register(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody TokenRequest request) {
        String token = request.getToken();

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String login = jwtUtil.extractUsername(token);
        Role role = jwtUtil.extractClaim(token, claims -> Role.valueOf(claims.get("role", String.class)));
        String newToken = jwtUtil.generateToken(login, role);

        return ResponseEntity.ok(new AuthResponse(newToken));
    }
}
