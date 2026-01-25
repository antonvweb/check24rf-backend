package org.example.mcoService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_binding_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBindingStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "binding_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BindingStatus bindingStatus;

    @Column(name = "partner_connected")
    private Boolean partnerConnected;

    @Column(name = "receipts_enabled")
    private Boolean receiptsEnabled;

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled;

    @Column(name = "bound_at")
    private LocalDateTime boundAt;

    @Column(name = "unbound_at")
    private LocalDateTime unboundAt;

    @Column(name = "last_status_check")
    private LocalDateTime lastStatusCheck;

    @Column(name = "consent_timestamp")
    private LocalDateTime consentTimestamp;
    @Column(name = "consent_source")
    private String consentSource;
    @Column(name = "consent_version")
    private String consentVersion;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "bound")
    private boolean bound;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BindingStatus {
        PENDING,
        IN_PROGRESS,
        APPROVED,
        DECLINED,
        EXPIRED,
        UNBOUND,
        CANCELLED
    }
}