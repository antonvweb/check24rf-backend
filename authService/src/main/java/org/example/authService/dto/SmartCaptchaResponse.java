package org.example.authService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartCaptchaResponse {
    private String status;
    private String message;

    public boolean isValid() {
        return "ok".equals(status);
    }
}
