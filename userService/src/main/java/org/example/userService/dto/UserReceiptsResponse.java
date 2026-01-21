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
 * Детальная информация о чеках пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReceiptsResponse {
    
    private UUID userId;
    private String phoneNumber;
    private List<ReceiptDetail> receipts;
    private PaginationInfo pagination;
    private ReceiptsSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptDetail {
        private UUID id;
        private String fiscalDriveNumber;
        private Long fiscalDocumentNumber;
        private Long fiscalSign;
        private LocalDateTime receiptDateTime;
        private LocalDateTime receiveDate;
        private BigDecimal totalSum;
        private String sourceCode;
        private String retailPlace;
        private String userInn;
        private Integer operationType;
        // rawJson можно добавить при необходимости
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptsSummary {
        private long totalCount;
        private BigDecimal totalAmount;
        private BigDecimal averageAmount;
        private LocalDateTime oldestReceiptDate;
        private LocalDateTime newestReceiptDate;
    }
}
