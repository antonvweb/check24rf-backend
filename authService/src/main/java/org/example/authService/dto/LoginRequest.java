package org.example.authService.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
