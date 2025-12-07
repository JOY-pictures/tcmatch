package com.tcmatch.tcmatch.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@RequiredArgsConstructor
@Slf4j
public enum SubscriptionTier {

    // 🔥 Вот твои тарифы, которые мы обсудили:
    FREE(
            "Бесплатный",
            3,
            false,              // Без мгновенных уведомлений
            false,              // Без приоритета
            0.0                 // Бесплатно
    ),
    BASIC(
            "⭐ Базовый (Basic)",
            15,
            true,              // ✅ Мгновенные уведомления
            false,              // Без приоритета
            159.0               // 399 руб
    ),
    PRO(
            "💎 Профессиональный (Pro)",
            Integer.MAX_VALUE,
            true,               // ✅ Мгновенные уведомления
            true,               // ✅ Приоритет в списке
            399.0              // 699 руб
    );

    // 🔥 НОВОЕ ПОЛЕ: Читаемое имя
    private final String displayName;

    // Параметры каждого тарифа
    private final int monthlyApplicationLimit;
    private final boolean hasInstantNotifications;
    private final boolean hasPriorityVisibility;
    private final double price;

    /**
     * Вспомогательный метод для безопасного получения Tier по названию (например, из callback'а)
     */
    public static SubscriptionTier fromName(String name) {
        if (name == null) {
            return FREE;
        }
        try {
            return SubscriptionTier.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid SubscriptionTier name: {}", name);
            return FREE; // По умолчанию всегда FREE
        }
    }
}