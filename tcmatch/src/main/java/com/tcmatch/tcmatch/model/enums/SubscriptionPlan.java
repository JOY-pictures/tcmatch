package com.tcmatch.tcmatch.model.enums;

import lombok.Getter;

@Getter
public enum SubscriptionPlan {
    FREE("Бесплатный", 3, 0.0, 30, false),           // 3 откликов в МЕСЯЦ
    BASIC("Базовый", 10, 399.0, 30, false),          // 10 откликов в месяц
    PRO("Профессиональный", 20, 799.0, 30, true),  // 20 откликов в месяц
    UNLIMITED("Безлимитный", 9999, 1499.0, 30, true); // Практически безлимит

    private final String name;
    private final int monthlyApplicationsLimit;
    private final double monthlyPrice;
    private final int subscriptionDays;
    private final boolean instantNotifications; // 🔥 Мгновенные уведомления о новых проектах

    SubscriptionPlan(String name, int monthlyApplicationsLimit, double monthlyPrice, int subscriptionDays, boolean instantNotifications) {
        this.name = name;
        this.monthlyApplicationsLimit = monthlyApplicationsLimit;
        this.monthlyPrice = monthlyPrice;
        this.subscriptionDays = subscriptionDays;
        this.instantNotifications = instantNotifications;
    }

    public String getDisplayName() {
        return String.format("%s - %d откликов/месяц", name, monthlyApplicationsLimit);
    }

    public String getPriceDisplay() {
        return monthlyPrice > 0 ? String.format("%.0f руб/мес", monthlyPrice) : "Бесплатно";
    }

    /**
     * 🔥 ПРОВЕРЯЕТ, ВКЛЮЧЕНЫ ЛИ МГНОВЕННЫЕ УВЕДОМЛЕНИЯ
     */
    public boolean hasInstantNotifications() {
        return instantNotifications;
    }
}
