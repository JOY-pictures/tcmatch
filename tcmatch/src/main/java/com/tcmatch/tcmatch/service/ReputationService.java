package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReputationService {

    private final UserRepository userRepository;

    // 🔥 ОСНОВНОЙ МЕТОД ОБНОВЛЕНИЯ РЕЙТИНГА
    @Transactional
    public void updateUserReputation(Long userId, Long projectId, boolean isSuccessful,
                                     boolean isOnTime, Double projectBudget,
                                     boolean hasArbitration, boolean isArbitrationLost) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 1. БАЗОВЫЕ БАЛЛЫ (зависит от бюджета)
        double basePoints = calculateBasePoints(projectBudget);

        // 2. БОНУС ЗА СРОКИ (+20%)
        if (isOnTime) {
            basePoints *= 1.2;
            user.setOnTimeProjectsCount(user.getOnTimeProjectsCount() + 1);
        }

        // 3. ШТРАФЫ ЗА АРБИТРАЖ
        if (hasArbitration && isArbitrationLost) {
            basePoints = -basePoints * 8; // Штраф в 8 раз больше
            log.warn("⚖️ Штрафные баллы пользователю {} за проигранный арбитраж: {}", userId, basePoints);
        }

        // 4. ОБНОВЛЕНИЕ СТАТИСТИКИ
        user.setTotalProjectsCount(user.getTotalProjectsCount() + 1);

        if (isSuccessful) {
            user.setSuccessfulProjectsCount(user.getSuccessfulProjectsCount() + 1);
            user.setCompletedProjectsCount(user.getCompletedProjectsCount() + 1);

            // Обновляем ПРП
            double newRating = user.getProfessionalRating() + basePoints;
            user.setProfessionalRating(Math.max(0, newRating)); // Рейтинг не может быть отрицательным
        }

        // 5. ПЕРЕСЧЕТ КОЭФФИЦИЕНТОВ
        recalculateCoefficients(user);

        userRepository.save(user);
        log.info("✅ Обновлен рейтинг пользователя {}: ПРП={}, КУЗ={}%, КС={}%",
                userId, user.getProfessionalRating(), user.getSuccessRate(), user.getTimelinessRate());
    }

    // 📊 РАСЧЕТ БАЗОВЫХ БАЛЛОВ (зависит от бюджета)
    private double calculateBasePoints(Double projectBudget) {
        if (projectBudget == null) return 10.0;

        if (projectBudget < 5000) {
            return 10.0; // Малобюджетные проекты
        } else if (projectBudget < 20000) {
            return 25.0; // Средние проекты
        } else if (projectBudget < 50000) {
            return 60.0; // Крупные проекты
        } else {
            return 150.0; // Премиум проекты
        }
    }

    // 📈 ПЕРЕСЧЕТ КОЭФФИЦИЕНТОВ
    private void recalculateCoefficients(User user) {
        // Коэффициент Успешного Завершения (КУЗ)
        if (user.getTotalProjectsCount() > 0) {
            double successRate = (user.getSuccessfulProjectsCount() * 100.0) / user.getTotalProjectsCount();
            user.setSuccessRate(Math.round(successRate * 10.0) / 10.0); // Округление до 0.1
        }

        // Коэффициент Своевременности (КС)
        if (user.getCompletedProjectsCount() > 0) {
            double timelinessRate = (user.getOnTimeProjectsCount() * 100.0) / user.getCompletedProjectsCount();
            user.setTimelinessRate(Math.round(timelinessRate * 10.0) / 10.0);
        }
    }

    // ⚠️ ПОМЕТКА НА ПРОВЕРКУ (при подозрительной активности)
    @Transactional
    public void markForReview(Long userId, String reason, int days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setIsUnderReview(true);
        user.setReviewReason(reason);
        user.setReviewUntil(LocalDateTime.now().plusDays(days));

        userRepository.save(user);
        log.warn("⚠️ Пользователь {} помечен на проверку по причине: {}", userId, reason);
    }
}
