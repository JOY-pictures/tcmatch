package com.tcmatch.tcmatch.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Статус Заказа.
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    // Заказ активен, работа идет
    ACTIVE("Активен"),

    // Заказ завершен и оплачен
    COMPLETED("Завершен"),

    // Заказ отменен по согласию сторон или арбитражу
    CANCELLED("Отменен"),

    // Ожидает финального подтверждения (для сложных схем Escrow)
    PENDING_CONFIRMATION("Ожидает подтверждения");

    private final String displayName;
}