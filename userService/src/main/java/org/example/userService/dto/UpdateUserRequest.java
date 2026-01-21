package org.example.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на обновление информации пользователя
 * Все поля опциональны - обновляются только переданные
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @Pattern(regexp = "^7\\d{10}$", message = "Альтернативный номер телефона должен начинаться с 7 и содержать 11 цифр")
    private String phoneNumberAlt;
    
    @Email(message = "Некорректный формат email")
    private String email;
    
    @Email(message = "Некорректный формат альтернативного email")
    private String emailAlt;
    
    private String telegramChatId;
    
    private Boolean isActive;
}
