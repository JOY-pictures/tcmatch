package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.enums.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserService userService;

    // 🔥 Храним информацию о использованных откликах
    private final Map<Long, UserSubscriptionInfo> userSubscriptions = new ConcurrentHashMap<>();

    public static class UserSubscriptionInfo {
        public SubscriptionPlan plan;
        public int usedApplications;
        public LocalDateTime subscriptionStartDate;
        public LocalDateTime subscriptionEndDate;
        public boolean isActive;

        public UserSubscriptionInfo(SubscriptionPlan plan) {
            this.plan = plan;
            this.usedApplications = 0;
            this.subscriptionStartDate = LocalDateTime.now();
            this.subscriptionEndDate = LocalDateTime.now().plusDays(plan.getSubscriptionDays());
            this.isActive = true;
        }

        // 🔥 ПОЛУЧЕНИЕ ОСТАВШИХСЯ ОТКЛИКОВ
        public int getRemainingApplications() {
            return Math.max(0, plan.getMonthlyApplicationsLimit() - usedApplications);
        }

        // 🔥 ПРОВЕРКА МОЖНО ЛИ ИСПОЛЬЗОВАТЬ ОТКЛИК
        public boolean canUseApplication() {
            return isActive && getRemainingApplications() > 0;
        }
    }

    // 🔥 ПРОВЕРКА ДОСТУПНЫХ ОТКЛИКОВ
    public SubscriptionCheckResult checkApplicationLimits(Long chatId) {
        UserSubscriptionInfo subscription = getUserSubscriptionInfo(chatId);

        boolean hasSubscription = subscription.isActive;
        int remainingApplications = subscription.getRemainingApplications();
        boolean canApply = subscription.canUseApplication();

        return new SubscriptionCheckResult(canApply, hasSubscription, remainingApplications, subscription.plan);
    }

    // 🔥 ИСПОЛЬЗОВАНИЕ ОТКЛИКА
    public boolean useApplication(Long chatId) {
        UserSubscriptionInfo subscription = getUserSubscriptionInfo(chatId);

        if (!subscription.canUseApplication()) {
            return false;
        }

        subscription.usedApplications++;
        userSubscriptions.put(chatId, subscription);

        log.info("📨 Пользователь {} использовал отклик. Использовано: {}/{}, Осталось: {}",
                chatId,
                subscription.usedApplications,
                subscription.plan.getMonthlyApplicationsLimit(),
                subscription.getRemainingApplications());

        return true;
    }

    // 🔥 ПОЛУЧЕНИЕ ИНФОРМАЦИИ О ПОДПИСКЕ
    private UserSubscriptionInfo getUserSubscriptionInfo(Long chatId) {
        return userSubscriptions.computeIfAbsent(chatId, k -> {
            // 🔥 ПО УМОЛЧАНИЮ - БЕСПЛАТНЫЙ ТАРИФ (3 отклика в месяц)
            return new UserSubscriptionInfo(SubscriptionPlan.FREE);
        });
    }

    // 🔥 ОБНОВЛЕНИЕ ПОДПИСКИ
    public void updateSubscription(Long chatId, SubscriptionPlan newPlan) {
        UserSubscriptionInfo newSubscription = new UserSubscriptionInfo(newPlan);
        userSubscriptions.put(chatId, newSubscription);
        log.info("💎 Пользователь {} перешел на тариф: {}", chatId, newPlan.getName());
    }

    // 🔥 СБРОС МЕСЯЧНЫХ ЛИМИТОВ (будет вызываться 1 числа каждого месяца)
    public void resetMonthlyLimits() {
        userSubscriptions.forEach((chatId, subscription) -> {
            if (subscription.isActive) {
                subscription.usedApplications = 0;
                subscription.subscriptionStartDate = LocalDateTime.now();
                subscription.subscriptionEndDate = LocalDateTime.now().plusDays(subscription.plan.getSubscriptionDays());
            }
        });
        log.info("🔄 Сброшены месячные лимиты откликов");
    }

    // 🔥 ПОЛУЧЕНИЕ СТАТИСТИКИ ПОЛЬЗОВАТЕЛЯ
    public UserSubscriptionStats getUserStats(Long chatId) {
        UserSubscriptionInfo subscription = getUserSubscriptionInfo(chatId);

        return new UserSubscriptionStats(
                subscription.plan,
                subscription.usedApplications,
                subscription.getRemainingApplications(),
                subscription.subscriptionStartDate,
                subscription.subscriptionEndDate
        );
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
