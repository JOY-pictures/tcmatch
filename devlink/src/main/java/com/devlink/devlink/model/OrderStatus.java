package com.devlink.devlink.model;

public enum OrderStatus {
    CREATED,
    IN_PROGRESS,
    AWAITING_CLARIFICATION,     // Ожидание уточнения от заказчика
    UNDER_REVIEW,
    REVISION,
    COMPLETED,
    CANCELLED,
    DISPUTE                      // Открыт спор
}
