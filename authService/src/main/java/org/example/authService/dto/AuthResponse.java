package org.example.authService.dto;

public class AuthResponse {
    private String token;
    private String userId;

    public AuthResponse(String token, String userId) {
        this.token = token;
        this.userId = userId;
    }

    // геттеры и сеттеры
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

