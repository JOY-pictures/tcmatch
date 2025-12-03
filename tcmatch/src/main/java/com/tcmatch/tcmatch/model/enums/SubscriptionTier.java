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
            5,                  // 5 откликов
            false,              // Без мгновенных уведомлений
            false,              // Без приоритета
            0.0                 // Бесплатно
    ),
    BASIC(
            "⭐ Базовый (Basic)",
            25,                 // 25 откликов
            false,              // Без мгновенных уведомлений
            false,              // Без приоритета
            399.0               // 399 руб
    ),
    PRO(
            "💎 Профессиональный (Pro)",
            75,                 // 75 откликов
            true,               // ✅ Мгновенные уведомления
            true,               // ✅ Приоритет в списке
            699.0               // 699 руб
    ),
    UNLIMITED(
            "👑 Безлимитный (Unlimited)",
            Integer.MAX_VALUE,  // Бесконечные отклики
            true,               // ✅ Мгновенные уведомления
            true,               // ✅ Приоритет в списке
            1599.0              // 1599 руб
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