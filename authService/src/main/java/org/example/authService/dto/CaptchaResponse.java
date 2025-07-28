package org.example.authService.dto;

public class CaptchaResponse {
    private String status;   // например "ok" или "fail"
    private String host;
    private String message;

    public boolean isValid() {
        return "ok".equalsIgnoreCase(status);
    }

    // геттеры и сеттеры, toString() для логирования
    @Override
    public String toString() {
        return "SmartCaptchaResponse{" +
                "status='" + status + '\'' +
                ", host='" + host + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
