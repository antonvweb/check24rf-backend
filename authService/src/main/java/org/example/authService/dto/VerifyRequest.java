package org.example.authService.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyRequest {
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Phone number is required")
    private String code;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCode() {
        return code;
    }
}
