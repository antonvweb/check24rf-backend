package org.example.mcoService.dto.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDto {

    private String phone;
    private String email;

    private Long fiscalSign;
    private Long fiscalDocumentNumber;
    private String fiscalDriveNumber;

    private LocalDateTime receiptDateTime;
    private LocalDateTime receiveDate;

    private BigDecimal totalSum;

    private String sourceCode;
    private Integer operationType;
    private String userInn;
    private String retailPlace;

    @JsonRawValue
    private String rawJson;
}