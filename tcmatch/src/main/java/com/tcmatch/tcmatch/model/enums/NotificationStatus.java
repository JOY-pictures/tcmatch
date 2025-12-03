package com.tcmatch.tcmatch.model.enums;

public enum NotificationStatus {
    UNREAD, // Не прочитано (должно быть в пуше)
    READ,   // Прочитано
    DELETED // Удалено пользователем (можно удалить из БД, но лучше пометить)
}
