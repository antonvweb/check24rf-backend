package org.example.mcoService.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Стандартный ответ API для успешных операций
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Успешность операции
     */
    private boolean success;

    /**
     * Сообщение для пользователя
     */
    private String message;

    /**
     * Данные ответа
     */
    private T data;

    /**
     * Время ответа
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Код ошибки (если есть)
     */
    private String errorCode;

    /**
     * Детали ошибки (для разработки)
     */
    private String errorDetails;

    /**
     * Создать успешный ответ с данными
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Создать успешный ответ с данными и сообщением
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Создать ответ об ошибке
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Создать ответ об ошибке с деталями
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, String errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .build();
    }
}