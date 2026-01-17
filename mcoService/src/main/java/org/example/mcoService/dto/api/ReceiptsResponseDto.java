package org.example.mcoService.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа при получении чеков
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiptsResponseDto {

    /**
     * Список чеков
     */
    private List<ReceiptDto> receipts;

    /**
     * Маркер для следующей порции
     */
    private String nextMarker;

    /**
     * Общее количество чеков в ответе
     */
    private Integer totalCount;

    /**
     * Количество оставшихся порций для загрузки
     */
    private Long remainingPolls;

    /**
     * Информационное сообщение
     */
    private String info;

    /**
     * Отдельный чек
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReceiptDto {
        private String userIdentifier;
        private String phone;
        private String email;
        private byte[] json;
        private LocalDateTime receiveDate;
        private String sourceCode;
    }
}