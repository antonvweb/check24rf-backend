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
    
    // Основная информация
    private UUID id;
    private String phoneNumber;
    private String phoneNumberAlt;
    private String email;
    private String emailAlt;
    private String telegramChatId;
    private LocalDateTime createdAt;
    private boolean isActive;
    
    // Статистика по чекам
    private UserStatistics statistics;
    
    // Последние чеки (например, 10 последних)
    private List<ReceiptSummary> recentReceipts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistics {
        private long totalReceiptsCount;        // Всего чеков
        private BigDecimal totalAmount;         // Общая сумма покупок
        private BigDecimal averageAmount;       // Средний чек
        private BigDecimal maxAmount;           // Максимальный чек
        private BigDecimal minAmount;           // Минимальный чек
        private LocalDateTime firstReceiptDate; // Дата первого чека
        private LocalDateTime lastReceiptDate;  // Дата последнего чека
        private long receiptsThisMonth;         // Чеков в этом месяце
        private BigDecimal amountThisMonth;     // Сумма в этом месяце
        private long receiptsThisYear;          // Чеков в этом году
        private BigDecimal amountThisYear;      // Сумма в этом году
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptSummary {
        private UUID id;
        private String fiscalDriveNumber;
        private Long fiscalDocumentNumber;
        private Long fiscalSign;
        private LocalDateTime receiptDateTime;
        private BigDecimal totalSum;
        private String retailPlace;
        private String sourceCode;
    }
}
