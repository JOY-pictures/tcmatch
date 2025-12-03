package com.tcmatch.tcmatch.model.enums;

public enum TransactionStatus {
    PENDING,        // Ожидает оплаты/подтверждения
    SUCCEEDED,      // Успешно оплачен, подписка активирована
    CANCELED,       // Отменен пользователем или истек срок
    ERROR,          // Ошибка обработки
    WAITING_FOR_CAPTURE // (Для 2-стадийного платежа, но мы используем 1-стадийный)
}