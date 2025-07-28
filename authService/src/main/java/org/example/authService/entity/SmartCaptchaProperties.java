package org.example.authService.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "smartcaptcha")
@Data
@Component
public class SmartCaptchaProperties {
    private String serverKey;
    private String validateUrl;

    public String getServerKey() {
        return serverKey;
    }

    public String getValidateUrl() {
        return validateUrl;
    }
}
