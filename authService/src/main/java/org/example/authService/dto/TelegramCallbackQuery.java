package org.example.authService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Callback query от Telegram
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramCallbackQuery {

    @JsonProperty("id")
    private String id;

    @JsonProperty("from")
    private TelegramUserDto from;

    @JsonProperty("message")
    private TelegramMessage message;

    @JsonProperty("data")
    private String data;
}
