package org.example.billingService.entity;

public enum SubscriptionType {
    ONE_MONTH(1, 999), // цена в копейках
    THREE_MONTHS(3, 2499),
    SIX_MONTHS(6, 4499),
    ONE_YEAR(12, 7999);

    private final int months;
    private final int priceInCents;

    SubscriptionType(int months, int priceInCents) {
        this.months = months;
        this.priceInCents = priceInCents;
    }

    public int getMonths() {
        return months;
    }

    public int getPriceInCents() {
        return priceInCents;
    }

    public double getPriceInRubles() {
        return priceInCents / 100.0;
    }
}