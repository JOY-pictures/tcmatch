package com.tcmatch.tcmatch.model.enums;

import lombok.Getter;

@Getter
public enum SubscriptionPlan {
    FREE("Бесплатный", 3, 0.0, 30),           // 3 откликов в МЕСЯЦ
    BASIC("Базовый", 10, 299.0, 30),          // 10 откликов в месяц
    PRO("Профессиональный", 20, 799.0, 30),  // 20 откликов в месяц
    UNLIMITED("Безлимитный", 9999, 1499.0, 30); // Практически безлимит

    private final String name;
    private final int monthlyApplicationsLimit;
    private final double monthlyPrice;
    private final int subscriptionDays;

    SubscriptionPlan(String name, int monthlyApplicationsLimit, double monthlyPrice, int subscriptionDays) {
        this.name = name;
        this.monthlyApplicationsLimit = monthlyApplicationsLimit;
        this.monthlyPrice = monthlyPrice;
        this.subscriptionDays = subscriptionDays;
    }

    public String getDisplayName() {
        return String.format("%s - %d откликов/месяц", name, monthlyApplicationsLimit);
    }

    public String getPriceDisplay() {
        return monthlyPrice > 0 ? String.format("%.0f руб/мес", monthlyPrice) : "Бесплатно";
    }
}
