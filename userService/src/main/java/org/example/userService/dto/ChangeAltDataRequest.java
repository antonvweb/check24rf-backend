package org.example.userService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public class ChangeAltDataRequest {
    @NotBlank(message = "Тип изменения не может быть пустым")
    @Pattern(regexp = "phone|email", message = "Тип должен быть 'phone' или 'email'")
    private String type;

    @NotBlank(message = "Данные не могут быть пустыми")
    private String data;

    // Конструкторы
    public ChangeAltDataRequest() {}

    public ChangeAltDataRequest(String type, String data) {
        this.type = type;
        this.data = data;
    }

    // Геттеры и сеттеры
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
