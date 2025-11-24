package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserService userService;

    // 🔥 Храним информацию о использованных откликах
//    private final Map<Long, UserSubscriptionInfo> userSubscriptions = new ConcurrentHashMap<>();

    /**
     * 🔥 ПОЛУЧЕНИЕ ТАРИФА ПОЛЬЗОВАТЕЛЯ
     */
    public SubscriptionPlan getUserSubscriptionPlan(Long chatId) {
        User user = userService.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔥 Если платная подписка истекла - возвращаем на FREE
        if (user.getSubscriptionPlan() != SubscriptionPlan.FREE &&
                !isSubscriptionActive(user)) {
            downgradeToFreePlan(user);
            return SubscriptionPlan.FREE;
        }

        return user.getSubscriptionPlan();
    }

    /**
     * 🔥 ОБНОВЛЕНИЕ ПОДПИСКИ ПОЛЬЗОВАТЕЛЯ
     */
    @Transactional
    public void updateUserSubscription(Long chatId, SubscriptionPlan newPlan) {
        User user = userService.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionPlan(newPlan);
        user.setUsedApplications(0);
        user.setPeriodStart(LocalDateTime.now());
        user.setPeriodEnd(LocalDateTime.now().plusDays(newPlan.getSubscriptionDays()));
        user.setSubscriptionExpiresAt(user.getPeriodEnd());
        user.setUpdatedAt(LocalDateTime.now());

        userService.updateUser(user); // 🔥 ИСПОЛЬЗУЕМ UserService

        log.info("💎 Пользователь {} перешел на тариф: {}", chatId, newPlan.getDisplayName());
    }

    /**
     * 🔥 ПРОВЕРКА АКТИВНОСТИ ПОДПИСКИ (ОБНОВЛЕННАЯ)
     */
    private boolean isSubscriptionActive(User user) {
        // 🔥 FREE тариф всегда активен (бессрочный)
        if (user.getSubscriptionPlan() == SubscriptionPlan.FREE) {
            return true;
        }

        // 🔥 Для платных тарифов проверяем срок действия
        if (user.getSubscriptionExpiresAt() == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(user.getSubscriptionExpiresAt());
    }

    /**
     * 🔥 ПОЛУЧЕНИЕ ОСТАВШИХСЯ ОТКЛИКОВ
     */
    private int getRemainingApplications(User user) {
        SubscriptionPlan plan = getUserSubscriptionPlan(user.getChatId());
        return Math.max(0, plan.getMonthlyApplicationsLimit() - user.getUsedApplications());
    }

    /**
     * 🔥 ПРОВЕРКА ВОЗМОЖНОСТИ ИСПОЛЬЗОВАТЬ ОТКЛИК (ИСПРАВЛЕННАЯ ЛОГИКА)
     */
    private boolean canUseApplication(User user) {
        // 🔥 Пользователь ВСЕГДА может использовать отклики в рамках своего тарифа
        return getRemainingApplications(user) > 0;
    }

    /**
     * 🔥 ПРОВЕРКА ДОСТУПНЫХ ОТКЛИКОВ
     */
    public SubscriptionCheckResult checkApplicationLimits(Long chatId) {
        User user = userService.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPlan plan = getUserSubscriptionPlan(chatId);
        int remainingApplications = getRemainingApplications(user);
        boolean canApply = remainingApplications > 0;

        return new SubscriptionCheckResult(canApply, plan != SubscriptionPlan.FREE,
                remainingApplications, plan);
    }

    /**
     * 🔥 ИСПОЛЬЗОВАНИЕ ОТКЛИКА
     */
    @Transactional
    public boolean useApplication(Long chatId) {
        User user = userService.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (getRemainingApplications(user) <= 0) {
            return false;
        }

        user.setUsedApplications(user.getUsedApplications() + 1);
        userService.updateUser(user);

        log.info("📨 Пользователь {} использовал отклик. Использовано: {}/{}, Осталось: {}",
                chatId, user.getUsedApplications(),
                getUserSubscriptionPlan(chatId).getMonthlyApplicationsLimit(),
                getRemainingApplications(user));

        return true;
    }

    /**
     * 🔥 СБРОС МЕСЯЧНЫХ ЛИМИТОВ (будет вызываться 1 числа каждого месяца)
     */
    @Transactional
    public void resetMonthlyLimits() {
        // 🔥 ИСПОЛЬЗУЕМ UserService для получения всех пользователей
        List<User> allUsers = userService.getAllUsers();

        List<User> usersWithActiveSubscriptions = allUsers.stream()
                .filter(this::isSubscriptionActive)
                .collect(Collectors.toList());

        for (User user : usersWithActiveSubscriptions) {
            user.setUsedApplications(0);
            user.setPeriodStart(LocalDateTime.now());
            user.setPeriodEnd(LocalDateTime.now().plusDays(user.getSubscriptionPlan().getSubscriptionDays()));
            user.setUpdatedAt(LocalDateTime.now());
            userService.updateUser(user); // 🔥 ИСПОЛЬЗУЕМ UserService
        }

        log.info("🔄 Сброшены месячные лимиты откликов для {} пользователей", usersWithActiveSubscriptions.size());
    }

    /**
     * 🔥 ПОЛУЧЕНИЕ СТАТИСТИКИ ПОЛЬЗОВАТЕЛЯ
     */
    public UserSubscriptionStats getUserStats(Long chatId) {
        User user = userService.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserSubscriptionStats(
                user.getSubscriptionPlan(),
                user.getUsedApplications(),
                getRemainingApplications(user),
                user.getPeriodStart(),
                user.getPeriodEnd()
        );
    }

    /**
     * 🔥 ПЕРЕХОД НА FREE ТАРИФ
     */
    private void downgradeToFreePlan(User user) {
        user.setSubscriptionPlan(SubscriptionPlan.FREE);
        user.setUsedApplications(0);
        user.setSubscriptionExpiresAt(null);
        userService.updateUser(user);
        log.info("⬇️ Пользователь {} переведен на FREE тариф", user.getChatId());
    }

    // 🔥 РЕЗУЛЬТАТ ПРОВЕРКИ ПОДПИСКИ
    public static class SubscriptionCheckResult {
        public final boolean canApply;
        public final boolean hasActiveSubscription;
        public final int remainingApplications;
        public final SubscriptionPlan currentPlan;

        public SubscriptionCheckResult(boolean canApply, boolean hasActiveSubscription,
                                       int remainingApplications, SubscriptionPlan currentPlan) {
            this.canApply = canApply;
            this.hasActiveSubscription = hasActiveSubscription;
            this.remainingApplications = remainingApplications;
            this.currentPlan = currentPlan;
        }
    }

    // 🔥 СТАТИСТИКА ПОЛЬЗОВАТЕЛЯ
    public static class UserSubscriptionStats {
        public final SubscriptionPlan plan;
        public final int usedApplications;
        public final int remainingApplications;
        public final LocalDateTime periodStart;
        public final LocalDateTime periodEnd;

        public UserSubscriptionStats(SubscriptionPlan plan, int usedApplications,
                                     int remainingApplications, LocalDateTime periodStart, LocalDateTime periodEnd) {
            this.plan = plan;
            this.usedApplications = usedApplications;
            this.remainingApplications = remainingApplications;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
        }
    }
}
