package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // Найти подписку по ID пользователя
    Optional<Subscription> findByUserId(Long userChatId);

    // 🔥 Найти все подписки, которые истекли (для Scheduler'а)
    List<Subscription> findBySubscriptionEndsAtBefore(LocalDateTime now);
}
