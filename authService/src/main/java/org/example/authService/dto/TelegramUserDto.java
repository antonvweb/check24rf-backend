package org.example.authService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Пользователь Telegram (DTO)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("is_bot")
    private Boolean isBot;
}
