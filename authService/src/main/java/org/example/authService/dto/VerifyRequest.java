package org.example.authService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на верификацию кода
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRequest {
    
    @NotBlank(message = "Идентификатор (телефон или email) обязателен")
    private String identifier;
    
    @NotBlank(message = "Код подтверждения обязателен")
    private String code;
}
