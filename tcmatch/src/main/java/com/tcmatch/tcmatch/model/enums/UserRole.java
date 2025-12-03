package com.tcmatch.tcmatch.model.enums;

public enum UserRole {
    CUSTOMER,
    FREELANCER,
    ADMIN,
    UNREGISTERED;

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

    public enum ProjectCreationStep {
        TITLE,
        DESCRIPTION,
        BUDGET,
        DEADLINE,
        SKILLS,
        CONFIRMATION
    }

    public enum ApplicationStatus {
        PENDING,      // Ожидает рассмотрения
        ACCEPTED,     // Принят заказчиком
        REJECTED,     // Отклонен заказчиком
        WITHDRAWN     // Отозван исполнителем
    }

    public enum ProjectStatus {
        OPEN,
        IN_PROGRESS,
        UNDER_REVIEW,
        COMPLETED,
        CANCELLED,
        DISPUTE
    }

    public enum RegistrationStatus {
        NOT_REGISTERED,     // Пользователь только написал /start
        REGISTERED,         // Создан аккаунт (/register), но правила не видел
        ROLE_SELECTED,
        RULES_VIEWED,
        RULES_ACCEPTED  // Просмотрел правила (/rules), но не принял
    }

    public enum UserStatus {
        ACTIVE,
        BLOCKED,
        DELETED
    }
}
