package com.tcmatch.tcmatch.model.dto;

import com.tcmatch.tcmatch.model.enums.SubscriptionPlan;
import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private Long chatId;
    private String userName;
    private String firstName;
    private String lastName;
    private UserRole role;
    private UserRole.UserStatus status;
    private Double rating;
    private Double professionalRating;
    private String specialization;
    private String experienceLevel;
    private String skills;
    private Boolean isVerified;

    // 🔥 НОВЫЕ ПОЛЯ ДЛЯ СТАТИСТИКИ И РЕПУТАЦИИ
    private LocalDateTime registeredAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime rulesAcceptedAt;

    // 🔥 СИСТЕМА РЕПУТАЦИИ (ПРП)
    private Double successRate; // КУЗ - % успешных проектов
    private Double timelinessRate; // КС - % своевременных проектов

    // 🔥 СТАТИСТИКА ПРОЕКТОВ
    private Integer completedProjectsCount;
    private Integer successfulProjectsCount;
    private Integer onTimeProjectsCount;
    private Integer totalProjectsCount;

    // 🔥 ВЕРИФИКАЦИЯ И МОДЕРАЦИЯ
    private String verificationMethod;
    private LocalDateTime verifiedAt;
    private Boolean isUnderReview;
    private String reviewReason;
    private LocalDateTime reviewUntil;

    // 🔥 ПОЛЕ ПОДПИСКИ
    private SubscriptionPlan subscriptionPlan;
    private LocalDateTime subscriptionExpiresAt;

    public static UserDto fromEntity(com.tcmatch.tcmatch.model.User entity) {
        if (entity == null) return null;

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setChatId(entity.getChatId());
        dto.setUserName(entity.getUserName());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setRole(entity.getRole() != null ? entity.getRole() : null);
        dto.setStatus(entity.getStatus() != null ? entity.getStatus() : null);
        dto.setRating(entity.getRating());
        dto.setProfessionalRating(entity.getProfessionalRating());
        dto.setSpecialization(entity.getSpecialization());
        dto.setExperienceLevel(entity.getExperienceLevel());
        dto.setSkills(entity.getSkills());
        dto.setIsVerified(entity.getIsVerified());

        // 🔥 ДОБАВЛЯЕМ НОВЫЕ ПОЛЯ
        dto.setRegisteredAt(entity.getRegisteredAt());
        dto.setLastActivityAt(entity.getLastActivityAt());
        dto.setRulesAcceptedAt(entity.getRulesAcceptedAt());
        dto.setSuccessRate(entity.getSuccessRate());
        dto.setTimelinessRate(entity.getTimelinessRate());
        dto.setCompletedProjectsCount(entity.getCompletedProjectsCount());
        dto.setSuccessfulProjectsCount(entity.getSuccessfulProjectsCount());
        dto.setOnTimeProjectsCount(entity.getOnTimeProjectsCount());
        dto.setTotalProjectsCount(entity.getTotalProjectsCount());
        dto.setVerificationMethod(entity.getVerificationMethod());
        dto.setVerifiedAt(entity.getVerifiedAt());
        dto.setIsUnderReview(entity.getIsUnderReview());
        dto.setReviewReason(entity.getReviewReason());
        dto.setReviewUntil(entity.getReviewUntil());

        // 🔥 ДОБАВЛЯЕМ ПОДПИСКУ
        dto.setSubscriptionPlan(entity.getSubscriptionPlan());
        dto.setSubscriptionExpiresAt(entity.getSubscriptionExpiresAt());

        return dto;
    }

    // 🔥 ДОБАВЛЯЕМ МЕТОД ДЛЯ ПРОВЕРКИ ПРЕМИУМА
    public boolean isPremium() {
        return subscriptionPlan != null &&
                subscriptionPlan != SubscriptionPlan.FREE &&
                subscriptionPlan != SubscriptionPlan.BASIC;
    }

    public boolean hasActiveSubscription() {
        if (subscriptionExpiresAt == null) {
            return false;
        }
        return subscriptionExpiresAt.isAfter(LocalDateTime.now());
    }

    public String getDisplayName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (userName != null) {
            return "@" + userName;
        } else {
            return "Пользователь";
        }
    }

    // 🔥 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    /**
     * Рассчитывает процент успешных проектов
     */
    public double calculateSuccessPercentage() {
        if (totalProjectsCount == null || totalProjectsCount == 0) {
            return 0.0;
        }
        if (successfulProjectsCount == null) {
            return 0.0;
        }
        return (double) successfulProjectsCount / totalProjectsCount * 100;
    }

    /**
     * Рассчитывает процент своевременных проектов
     */
    public double calculateTimelinessPercentage() {
        if (totalProjectsCount == null || totalProjectsCount == 0) {
            return 0.0;
        }
        if (onTimeProjectsCount == null) {
            return 0.0;
        }
        return (double) onTimeProjectsCount / totalProjectsCount * 100;
    }

    /**
     * Проверяет активен ли пользователь (был онлайн в последние 7 дней)
     */
    public boolean isActive() {
        if (lastActivityAt == null) {
            return false;
        }
        return lastActivityAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * Получает статус активности
     */
    public String getActivityStatus() {
        if (isActive()) {
            return "🟢 Онлайн недавно";
        } else {
            return "⚪ Давно не в сети";
        }
    }

    /**
     * Получает информацию о верификации
     */
    public String getVerificationInfo() {
        if (isVerified == null || !isVerified) {
            return "⚪ Не верифицирован";
        }

        if (verificationMethod != null) {
            return String.format("✅ Верифицирован (%s)", getVerificationMethodDisplay());
        } else {
            return "✅ Верифицирован";
        }
    }

    private String getVerificationMethodDisplay() {
        if (verificationMethod == null) return "платформа";

        return switch (verificationMethod.toUpperCase()) {
            case "EMAIL" -> "email";
            case "PHONE" -> "телефон";
            case "DEPOSIT" -> "депозит";
            default -> "платформа";
        };
    }

    /**
     * Проверяет находится ли пользователь на модерации
     */
    public boolean isUnderReview() {
        return isUnderReview != null && isUnderReview;
    }
}