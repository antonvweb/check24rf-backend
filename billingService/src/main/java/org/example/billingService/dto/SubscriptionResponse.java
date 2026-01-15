package org.example.billingService.dto;

import org.example.billingService.entity.SubscriptionStatus;
import java.time.LocalDateTime;

public class SubscriptionResponse {
    private Long id;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private double pricePaid;
    private boolean isActive;
    private long daysLeft;

    public SubscriptionResponse() {}

    public SubscriptionResponse(Long id, SubscriptionStatus status,
                                LocalDateTime startDate, LocalDateTime endDate,
                                double pricePaid, boolean isActive, long daysLeft) {
        this.id = id;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pricePaid = pricePaid;
        this.isActive = isActive;
        this.daysLeft = daysLeft;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getDaysLeft() { return daysLeft; }
    public void setDaysLeft(long daysLeft) { this.daysLeft = daysLeft; }
}
