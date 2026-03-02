package org.example.authService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Чат Telegram
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramChat {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("username")
    private String username;
}
