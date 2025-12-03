package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long chatId;

    @Column(unique = true)
    private String userName;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserRole.UserStatus status;

    private Double rating;

    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole.RegistrationStatus registrationStatus =  UserRole.RegistrationStatus.NOT_REGISTERED;

    @Builder.Default
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    private LocalDateTime rulesViewedAt;
    private LocalDateTime rulesAcceptedAt;

    // 🔥 НОВЫЕ ПОЛЯ ДЛЯ СИСТЕМЫ РЕПУТАЦИИ
    @Builder.Default
    private Double professionalRating = 0.0; // ПРП - основной рейтинг

    // Коэффициенты
    @Builder.Default
    private Double successRate = 100.0; // КУЗ - % успешных проектов
    @Builder.Default
    private Double timelinessRate = 100.0; // КС - % своевременных проектов

    // Статистика
    private Integer completedProjectsCount = 0;
    private Integer successfulProjectsCount = 0;
    private Integer onTimeProjectsCount = 0;
    private Integer totalProjectsCount = 0;

    // Верификация
    @Builder.Default
    private Boolean isVerified = false;
    private String verificationMethod; // "EMAIL", "PHONE", "DEPOSIT"
    private LocalDateTime verifiedAt;

    // Защита от накрутки
    @Builder.Default
    private Boolean isUnderReview = false;
    private String reviewReason;
    private LocalDateTime reviewUntil;

    // Дополнительные профессиональные поля
    private String specialization; // "Backend", "Frontend", "Mobile", etc.
    private String experienceLevel; // "Junior", "Middle", "Senior"
    private String skills; // "Java, Spring, PostgreSQL, Docker"

    @Builder.Default
    private List<Long> favoriteProjects = new ArrayList<>();

    private LocalDateTime subscriptionExpiresAt;

    @Builder.Default
    private int usedApplications = 0;

    private LocalDateTime periodStart; // начало текущего периода
    private LocalDateTime periodEnd;   // конец текущего периода

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
