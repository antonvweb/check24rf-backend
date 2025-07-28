package org.example.authService.dto;

import jakarta.validation.constraints.NotBlank;

public class CaptchaRequest {
    @NotBlank(message = "Captcha token is required")
    private String captchaToken;

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }
}
