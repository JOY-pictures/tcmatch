package com.devlink.devlink.model;

public enum RegistrationStatus {
    NOT_REGISTERED,     // Пользователь только написал /start
    REGISTERED,         // Создан аккаунт (/register), но правила не видел
    RULES_VIEWED,       // Просмотрел правила (/rules), но не принял
    RULES_ACCEPTED      // Принял правила (/accept_rules) - полный доступ
}
