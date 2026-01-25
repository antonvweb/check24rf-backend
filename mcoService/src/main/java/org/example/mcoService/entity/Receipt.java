package org.example.mcoService.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_identifier", nullable = false, length = 20)
    private String userIdentifier;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "fiscal_sign", nullable = false)
    private Long fiscalSign;

    @Column(name = "fiscal_document_number", nullable = false)
    private Long fiscalDocumentNumber;

    @Column(name = "fiscal_drive_number", nullable = false, length = 50)
    private String fiscalDriveNumber;

    @Column(name = "receipt_date_time", nullable = false)
    private LocalDateTime receiptDateTime;

    @Column(name = "receive_date", nullable = false)
    private LocalDateTime receiveDate;

    @Column(name = "total_sum", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSum;

    @Column(name = "source_code", nullable = false, length = 50)
    private String sourceCode;

    @Column(name = "operation_type")
    private Integer operationType;

    @Column(name = "user_inn", length = 12)
    private String userInn;

    @Column(name = "retail_place", columnDefinition = "TEXT")
    private String retailPlace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    @JsonRawValue
    private String rawJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}