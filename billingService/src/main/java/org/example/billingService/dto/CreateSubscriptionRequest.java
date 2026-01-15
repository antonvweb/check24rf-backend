package org.example.billingService.dto;

import org.example.billingService.entity.SubscriptionType;

public class CreateSubscriptionRequest {
    private SubscriptionType subscriptionType;

    public CreateSubscriptionRequest() {}

    public CreateSubscriptionRequest(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }
}