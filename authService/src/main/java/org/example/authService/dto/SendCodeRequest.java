package org.example.authService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на отправку кода верификации
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeRequest {
    
    @NotBlank(message = "Идентификатор (телефон или email) обязателен")
    private String identifier; // Может быть телефон или email
    
    // Дополнительно можно добавить поле для явного указания типа
    // private String type; // "phone" или "email"
}
