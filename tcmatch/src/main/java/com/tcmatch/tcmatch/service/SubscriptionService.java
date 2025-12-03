package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.Subscription;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;

    @Transactional
    public void initializeNewUserSubscription(Long userId) {
        // Проверка на случай повторного вызова
        if (subscriptionRepository.findByUserId(userId).isPresent()) {
            log.warn("Attempt to initialize subscription for existing user: {}", userId);
            return;
        }
        // Используем конструктор, который устанавливает FREE лимиты
        Subscription freeSubscription = new Subscription(userId);
        subscriptionRepository.save(freeSubscription);
        log.info("Initialized FREE subscription for new user: {}", userId);
    }

    // =================================================================
    // 🔥 ВСПОМОГАТЕЛЬНЫЙ МЕТОД: Мост chatId -> userId
    // =================================================================
    private Long getUserIdByChatId(Long chatId) {
        // Предполагается, что userService имеет метод findByChatId, который возвращает Optional<User>
        return userService.findByChatId(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с chatId " + chatId + " не найден."))
                .getId();
    }

    /**
     * Получает текущую подписку пользователя.
     */
    public Subscription getSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Подписка пользователя не найдена: " + userId));
    }

    /**
     * 🔥 ГЛАВНЫЙ МЕТОД: Проверяет, достаточно ли у пользователя откликов для отправки нового.
     * @return true, если откликов > 0 или если тариф UNLIMITED.
     */
    public boolean hasSufficientApplications(Long chatId) {
        try {
            Long userId = getUserIdByChatId(chatId);
            Subscription sub = getSubscription(userId);

            // Если лимит Integer.MAX_VALUE (UNLIMITED), всегда true
            if (sub.getAvailableApplications() == Integer.MAX_VALUE) {
                return true;
            }
            return sub.getAvailableApplications() > 0;

        } catch (EntityNotFoundException e) {
            log.error("Subscription not found for user {}. Assuming 0 attempts.", chatId);
            return false;
        }
    }

    /**
     * 🔥 ГЛАВНЫЙ МЕТОД: Уменьшает количество доступных откликов на 1.
     * Должен вызываться после успешной проверки hasSufficientApplications.
     */
    @Transactional
    public void decrementApplicationCount(Long chatId) {
        Long userId = getUserIdByChatId(chatId);
        Subscription sub = getSubscription(userId);

        if (sub.getAvailableApplications() <= 0) {
            // Этого не должно случиться, но это защита
            throw new IllegalStateException("Нет доступных откликов для пользователя: " + userId);
        }

        // UNLIMITED не уменьшаем
        if (sub.getAvailableApplications() != Integer.MAX_VALUE) {
            sub.setAvailableApplications(sub.getAvailableApplications() - 1);
        }

        subscriptionRepository.save(sub);
        log.info("Decremented application count for user {}. Remaining: {}", userId, sub.getAvailableApplications());
    }

    /**
     * Логика покупки новой подписки (вызывается после успешной оплаты через YooMoney).
     */
    @Transactional
    public void upgradeSubscription(Long chatId, SubscriptionTier newTier) {
        Long userId = getUserIdByChatId(chatId);
        Subscription sub = getSubscription(userId);

        // 1. Определение даты начала новой подписки
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpiry = sub.getSubscriptionEndsAt();
        LocalDateTime subscriptionStart;

        // Если текущая подписка платная и еще не истекла (currentExpiry в будущем),
        // то новая подписка начинается сразу после истечения старой.
        if (currentExpiry != null && currentExpiry.isAfter(now) && sub.getTier() != SubscriptionTier.FREE) {
            subscriptionStart = currentExpiry;
            log.info("Продление: новая подписка начнется после истечения старой ({})", currentExpiry);
        } else {
            // Если подписка истекла или это первая платная подписка, начинаем сейчас.
            subscriptionStart = now;
            log.info("Покупка: новая подписка начинается сейчас.");
        }

        // 2. Расчет даты окончания (30 дней с даты начала)
        LocalDateTime newExpiry = subscriptionStart.plusDays(30);


        // 3. Обновление всех полей (Ваша существующая логика)
        sub.setTier(newTier);
        sub.setAvailableApplications(newTier.getMonthlyApplicationLimit()); // Обновляем лимит
        sub.setHasInstantNotifications(newTier.isHasInstantNotifications());
        sub.setHasPriorityVisibility(newTier.isHasPriorityVisibility());

        // 4. Устанавливаем дату окончания
        sub.setSubscriptionEndsAt(newExpiry);
        sub.setLastPaymentAt(now);

        subscriptionRepository.save(sub);
        log.info("User {} successfully upgraded to {}. Expires at {}", userId, newTier, sub.getSubscriptionEndsAt());    }

    // В будущем этот метод можно вызвать из планировщика (Scheduler), чтобы сбрасывать истекшие подписки
    @Transactional
    public void resetExpiredSubscription(Subscription sub) {
        if (sub.getTier() == SubscriptionTier.FREE) {
            return;
        }

        SubscriptionTier freeTier = SubscriptionTier.FREE;
        sub.setTier(freeTier);
        sub.setAvailableApplications(freeTier.getMonthlyApplicationLimit());
        sub.setHasInstantNotifications(freeTier.isHasInstantNotifications());
        sub.setHasPriorityVisibility(freeTier.isHasPriorityVisibility());
        sub.setSubscriptionEndsAt(null);

        subscriptionRepository.save(sub);
    }

    // 🔥 1. Вспомогательный класс для передачи статистики (замена SubscriptionCheckResult)
    @Data
    public static class SubscriptionStatsDto {
        private final SubscriptionTier tier;
        private final int remainingApplications;
        private final int monthlyLimit;
        private final LocalDateTime resetDate;

        public String formatResetDate() {
            return this.resetDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }

    public SubscriptionStatsDto getSubscriptionStats(Long chatId) {
        Long userId = getUserIdByChatId(chatId);
        Subscription sub = getSubscription(userId);
        SubscriptionTier tier = sub.getTier();

        int monthlyLimit = tier.getMonthlyApplicationLimit() == Integer.MAX_VALUE
                ? -1 // Условное обозначение UNLIMITED
                : tier.getMonthlyApplicationLimit();

        // Дата обновления: либо дата окончания (для платных), либо 1 число следующего месяца (для FREE/истекших)
        LocalDateTime resetDate = sub.getSubscriptionEndsAt() != null
                ? sub.getSubscriptionEndsAt()
                : LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);

        return new SubscriptionStatsDto(
                tier,
                sub.getAvailableApplications(),
                monthlyLimit,
                resetDate
        );
    }

    // 🔥 3. Замена метода useApplication на decrementApplicationCount
    // Так как твой ConfirmApplicationCommand использует useApplication,
    // давай создадим этот метод как обертку для чистоты кода.
    @Transactional
    public boolean useApplication(Long chatId) {
        try {
            decrementApplicationCount(chatId);
            return true;
        } catch (IllegalStateException e) {
            log.error("Failed to use application for user {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * 🔥 Проверяет, не истекла ли платная подписка, и возвращает активный Tier.
     * Вызывает resetExpiredSubscription, если необходимо.
     */
    @Transactional
    public SubscriptionTier getVerifiedSubscriptionTier(Long chatId) {
        Long userId = getUserIdByChatId(chatId);
        Subscription sub = getSubscription(userId);

        // 1. Проверяем, платная ли подписка и есть ли дата окончания
        if (sub.getTier() != SubscriptionTier.FREE && sub.getSubscriptionEndsAt() != null) {

            // 2. Проверяем, истекла ли подписка
            if (sub.getSubscriptionEndsAt().isBefore(LocalDateTime.now())) {

                // 3. Если истекла, сбрасываем ее на FREE.
                // Мы вызываем метод, который ты уже реализовал ранее (или должен был)
                resetExpiredSubscription(sub);

                return SubscriptionTier.FREE;
            }
        }

        // Если не платная или еще активна, возвращаем текущий Tier
        return sub.getTier();
    }

    // =================================================================
    // 🔥 МЕТОДЫ ДЛЯ ОТОБРАЖЕНИЯ (ИСПОЛЬЗУЮТ АКТУАЛЬНУЮ ЛОГИКУ ПОДПИСКИ)
    // =================================================================

    /**
     * Возвращает имя текущего тарифа пользователя.
     */
    public String getCurrentTariffName(Long chatId) {
        // Сначала проверяем, не истекла ли подписка, и получаем активный Tier.
        SubscriptionTier tier = getVerifiedSubscriptionTier(chatId);

        // Получаем дату окончания для отображения
        String endDateInfo = "";
        try {
            Subscription sub = getSubscription(getUserIdByChatId(chatId));
            if (sub.getSubscriptionEndsAt() != null) {
                endDateInfo = " (до " + sub.getSubscriptionEndsAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ")";
            }
        } catch (EntityNotFoundException ignored) {
            // Игнорируем.
        }

        return tier.getDisplayName() + endDateInfo;
    }

    /**
     * Возвращает лимит откликов в день (из Tier).
     */
    public int getDailyResponseLimit(Long chatId) {
        SubscriptionTier tier = getVerifiedSubscriptionTier(chatId);
        return tier.getMonthlyApplicationLimit(); // Используем лимит из Enum
    }

    /**
     * Возвращает, есть ли мгновенные уведомления (из Tier).
     */
    public boolean hasInstantMessaging(Long chatId) {
        SubscriptionTier tier = getVerifiedSubscriptionTier(chatId);
        return tier.isHasInstantNotifications();
    }

    /**
     * Возвращает, есть ли приоритет в поиске (из Tier).
     */
    public boolean hasSearchPriority(Long chatId) {
        SubscriptionTier tier = getVerifiedSubscriptionTier(chatId);
        return tier.isHasPriorityVisibility();
    }

    /**
     * Возвращает описание текущего тарифа пользователя.
     * (Теперь нужно вернуть список фич, так как нет поля description в Enum)
     */
    public String getTariffFeatures(Long chatId) {
        SubscriptionTier tier = getVerifiedSubscriptionTier(chatId);

        // Формируем читаемое описание на основе полей Tier
        String limit = tier.getMonthlyApplicationLimit() == Integer.MAX_VALUE ? "Безлимитно" : String.valueOf(tier.getMonthlyApplicationLimit());

        return String.format("""
            <b>Лимит откликов:</b> %s в месяц
            
            <b>Мгновенные уведомления:</b> %s
            
            <b>Приоритет в поиске:</b> %s
            """,
                limit,
                tier.isHasInstantNotifications() ? "✅ Включены" : "❌ Отключены",
                tier.isHasPriorityVisibility() ? "✅ Включен" : "❌ Отключен"
        );
    }

    // =================================================================
    // 🔥 ОБНОВЛЕННЫЕ МЕТОДЫ ДЛЯ ПОКУПКИ (для SelectSubscriptionCommand)
    // =================================================================

    /**
     * Получает все платные планы.
     */
    public List<SubscriptionTier> getAvailablePaidPlans() {
        return Arrays.stream(SubscriptionTier.values())
                .filter(tier -> tier.getPrice() > 0) // Фильтруем все, где цена > 0
                .collect(Collectors.toList());
    }

    /**
     * Получает план по ID (здесь ID - это Enum.ordinal(), или используем Enum.valueOf()).
     * Лучше использовать name:
     */
    public Optional<SubscriptionTier> getTierByName(String name) {
        try {
            return Optional.of(SubscriptionTier.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Data
    @Builder
    public static class SubscriptionInfo {
        private SubscriptionTier tier;
        private String displayName;
        private LocalDateTime endsAt;
        private Boolean isActive;
        private Long daysLeft;

        public String getFormattedEndsAt() {
            if (endsAt == null) return "Не ограничена";
            return endsAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }

        public String getDaysLeftText() {
            if (daysLeft == null) return "";
            if (daysLeft <= 0) return "истекла";
            if (daysLeft == 1) return "остался 1 день";
            return String.format("осталось %d дней", daysLeft);
        }
    }
}
