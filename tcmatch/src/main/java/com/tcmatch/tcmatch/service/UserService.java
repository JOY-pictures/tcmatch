package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private ProjectService projectService;
    private OrderService orderService;
    private ApplicationService applicationService;
    private ReputationService reputationService;
    private final UserRepository userRepository;

    public User registerFromTelegram(Long chatId, String username, String firstName, String lastName) {
        Optional<User> existingUser = userRepository.findByChatId(chatId);

        if (existingUser.isPresent()) {
            log.info("✅ user already exists: {}", existingUser.get());
            return existingUser.get();
        }

        User user = User.builder()
                .chatId(chatId)
                .username(username)
                .firstname(firstName)
                .lastname(lastName)
                .role(UserRole.FREELANCER)
                // 🔥 ИНИЦИАЛИЗИРУЕМ НОВЫЕ ПОЛЯ
                .professionalRating(0.0)
                .successRate(100.0)
                .timelinessRate(100.0)
                .completedProjectsCount(0)
                .successfulProjectsCount(0)
                .onTimeProjectsCount(0)
                .totalProjectsCount(0)
                .isVerified(false)
                .isUnderReview(false)
                .rating(0.0)
                .registrationStatus(UserRole.RegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);
        log.info("✅ Создан пользователь: {}", savedUser);
        return savedUser;
    }

    public Map<String, Object> getUserStatistics(Long chatId) {
        Map<String, Object> stats = new HashMap<>();
        User user = findByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        try {
            // 🔥 БЕЗОПАСНОЕ ПОЛУЧЕНИЕ ПРОЕКТОВ ПОЛЬЗОВАТЕЛЯ
            List<Project> userProjects = Collections.emptyList();
            if (projectService != null) {
                try {
                    userProjects = projectService.getUserProjects(chatId);
                } catch (Exception e) {
                    log.warn("⚠️ Не удалось получить проекты пользователя {}: {}", chatId, e.getMessage());
                    userProjects = Collections.emptyList();
                }
            } else {
                log.warn("⚠️ ProjectService is null для пользователя {}", chatId);
            }

            // 🔥 БЕЗОПАСНОЕ ПОЛУЧЕНИЕ АКТИВНЫХ ПРОЕКТОВ
            long activeProjects = 0L;
            if (!userProjects.isEmpty()) {
                activeProjects = userProjects.stream()
                        .filter(p -> p.getStatus() == UserRole.ProjectStatus.IN_PROGRESS)
                        .count();
            }

            // 🔥 БЕЗОПАСНОЕ ПОЛУЧЕНИЕ ДРУГОЙ СТАТИСТИКИ
            long activeOrders = 0L;
            long activeApplications = 0L;

            if (orderService != null) {
                try {
                    activeOrders = orderService.getActiveOrderCount(chatId);
                } catch (Exception e) {
                    log.warn("⚠️ Не удалось получить заказы пользователя {}: {}", chatId, e.getMessage());
                }
            }

            if (applicationService != null) {
                try {
                    activeApplications = applicationService.getActiveApplicationsCount(chatId);
                } catch (Exception e) {
                    log.warn("⚠️ Не удалось получить отклики пользователя {}: {}", chatId, e.getMessage());
                }
            }

            // 🔥 СТАТИСТИКА ИЗ ПОЛЕЙ USER (всегда доступна)
            stats.put("completedProjects", user.getCompletedProjectsCount() != null ? user.getCompletedProjectsCount() : 0);
            stats.put("successfulProjects", user.getSuccessfulProjectsCount() != null ? user.getSuccessfulProjectsCount() : 0);
            stats.put("onTimeProjects", user.getOnTimeProjectsCount() != null ? user.getOnTimeProjectsCount() : 0);
            stats.put("activeProjects", activeProjects);
            stats.put("activeOrders", activeOrders);
            stats.put("activeApplications", activeApplications);
            stats.put("totalProjects", user.getTotalProjectsCount() != null ? user.getTotalProjectsCount() : 0);
            stats.put("userRating", user.getProfessionalRating() != null ? user.getProfessionalRating() : 0.0);
            stats.put("successRate", user.getSuccessRate() != null ? user.getSuccessRate() : 100.0);
            stats.put("timelinessRate", user.getTimelinessRate() != null ? user.getTimelinessRate() : 100.0);

        } catch (Exception e) {
            log.error("❌ Критическая ошибка получения статистики для пользователя {}: {}", chatId, e.getMessage());
            // 🔥 ГАРАНТИРОВАННЫЕ ЗНАЧЕНИЯ ДАЖЕ ПРИ ОШИБКЕ
            stats.put("completedProjects", 0);
            stats.put("successfulProjects", 0);
            stats.put("onTimeProjects", 0);
            stats.put("activeProjects", 0L);
            stats.put("activeOrders", 0L);
            stats.put("activeApplications", 0L);
            stats.put("totalProjects", 0);
            stats.put("userRating", 0.0);
            stats.put("successRate", 100.0);
            stats.put("timelinessRate", 100.0);
        }

        return stats;
    }

    public User markRulesViewed(Long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRegistrationStatus(UserRole.RegistrationStatus.RULES_VIEWED);
        user.setRulesViewedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("📜 User viewed rules: {}", chatId);
        return savedUser;
    }
    public User acceptRules(Long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRegistrationStatus(UserRole.RegistrationStatus.RULES_ACCEPTED);
        user.setRulesAcceptedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("✅ User accepted rules: {}", chatId);
        return savedUser;
    }

    public boolean hasFullAccess(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(user -> user.getRegistrationStatus() == UserRole.RegistrationStatus.RULES_ACCEPTED)
                .orElse(false);
    }

    public UserRole.RegistrationStatus getRegistrationStatus(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getRegistrationStatus)
                .orElse(UserRole.RegistrationStatus.NOT_REGISTERED);
    }

    public Map<String, Object> getReputationStats(Long chatId) {
        User user = findByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Map<String, Object> reputationStats = new HashMap<>();
        reputationStats.put("professionalRating", user.getProfessionalRating());
        reputationStats.put("successRate", user.getSuccessRate());
        reputationStats.put("timelinessRate", user.getTimelinessRate());
        reputationStats.put("completedProjects", user.getCompletedProjectsCount());
        reputationStats.put("successfulProjects", user.getSuccessfulProjectsCount());
        reputationStats.put("onTimeProjects", user.getOnTimeProjectsCount());
        reputationStats.put("totalProjects", user.getTotalProjectsCount());
        reputationStats.put("isVerified", user.getIsVerified());
        reputationStats.put("isUnderReview", user.getIsUnderReview());

        return reputationStats;
    }

    // Метод для верификации пользователя
    @Transactional
    public User verifyUser(Long chatId, String verificationMethod) {
        User user = findByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setIsVerified(true);
        user.setVerificationMethod(verificationMethod);
        user.setVerifiedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("✅ Пользователь {} верифицирован методом: {}", chatId, verificationMethod);

        return savedUser;
    }

    // Метод для обновления профессиональной информации
    @Transactional
    public User updateProfessionalInfo(Long chatId, String specialization, String experienceLevel, String skills) {
        User user = findByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (specialization != null) user.setSpecialization(specialization);
        if (experienceLevel != null) user.setExperienceLevel(experienceLevel);
        if (skills != null) user.setSkills(skills);

        User savedUser = userRepository.save(user);
        log.info("✅ Обновлена проф. информация пользователя: {}", chatId);

        return savedUser;
    }

    public Optional<User> findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public boolean userExists(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }
}
