package org.example.userService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Полная информация о пользователе с чеками и статистикой
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

    private String phoneNumber;
    private String phoneNumberAlt;
    private String email;
    private String emailAlt;
    private String telegramChatId;
    private LocalDateTime createdAt;
    private boolean isActive;
    private boolean isPartnerConnected;
}
