package org.example.authService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO для входящих обновлений от Telegram Bot API
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdate {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private TelegramMessage message;

    @JsonProperty("callback_query")
    private TelegramCallbackQuery callbackQuery;
}
