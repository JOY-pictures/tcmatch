package com.tcmatch.tcmatch.model.enums;

public enum PurchaseType {
    SUBSCRIPTION("Подписка"),
    ORDER_CREATION("Создание заказа"),
    ORDER_ESCROW("Резервирование средств для заказа"),
    SERVICE_PAYMENT("Оплата услуги"),
    WITHDRAWAL("Вывод средств");

    private final String displayName;

    PurchaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}