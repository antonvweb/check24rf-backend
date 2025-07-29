package org.example.userService.dto;

public class ChangeAltDataResponse {
    private boolean success;
    private String message;
    private String type;
    private String newValue;

    public ChangeAltDataResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ChangeAltDataResponse(boolean success, String message, String type, String newValue) {
        this.success = success;
        this.message = message;
        this.type = type;
        this.newValue = newValue;
    }

    // Геттеры и сеттеры
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
}
