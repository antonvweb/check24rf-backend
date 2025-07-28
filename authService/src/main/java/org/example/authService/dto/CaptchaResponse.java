package org.example.authService.dto;

import lombok.Data;

@Data
public class CaptchaResponse {

    private String status;
    private String host;
    private String message;

    public boolean isValid() {
        return "ok".equalsIgnoreCase(status);
    }
}
