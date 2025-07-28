package org.example.authService.dto;

public class CaptchaResponse {

    private String status;
    private String host;
    private String message;

    public boolean isValid() {
        return "ok".equalsIgnoreCase(status);
    }

    public String getStatus() {
        return status;
    }

    public String getHost() {
        return host;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SmartCaptchaResponse{" +
                "status='" + status + '\'' +
                ", host='" + host + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
