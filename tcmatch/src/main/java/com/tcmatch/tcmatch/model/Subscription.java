package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с User (убедись, что User.id имеет тип Long)
    @Column(unique = true, nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;

    // 🔥 Счетчик оставшихся откликов
    @Column(nullable = false)
    private int availableApplications;

    // 🔥 Кэшированные права доступа (чтобы не дергать Enum)
    @Column(nullable = false)
    private boolean hasInstantNotifications;

    @Column(nullable = false)
    private boolean hasPriorityVisibility;

    // Дата, когда подписка истекает (и должна быть обновлена)
    private LocalDateTime subscriptionEndsAt;

    private LocalDateTime lastPaymentAt; // Дата последнего успешного платежа
    /**
     * Конструктор для создания новой (бесплатной) подписки при регистрации
     */
    public Subscription(Long userId) {
        this.userId = userId;
        this.tier = SubscriptionTier.FREE;
        this.availableApplications = SubscriptionTier.FREE.getMonthlyApplicationLimit();
        this.hasInstantNotifications = SubscriptionTier.FREE.isHasInstantNotifications();
        this.hasPriorityVisibility = SubscriptionTier.FREE.isHasPriorityVisibility();
        // subscriptionEndsAt = null, т.к. бесплатная вечная (или +30 дней, по твоей логике)
    }
}