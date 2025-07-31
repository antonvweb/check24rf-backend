package org.example.billingService.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
public class SubscriptionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "price_paid")
    private Integer pricePaid; // в копейках

    public SubscriptionData() {}

    public SubscriptionData(UUID userId, SubscriptionStatus status,
                        LocalDateTime startDate, LocalDateTime endDate, Integer pricePaid) {
        this.userId = userId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pricePaid = pricePaid;
        this.createdAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getPricePaid() { return pricePaid; }
    public void setPricePaid(Integer pricePaid) { this.pricePaid = pricePaid; }

    // Полезные методы
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE &&
                LocalDateTime.now().isBefore(endDate);
    }
}
