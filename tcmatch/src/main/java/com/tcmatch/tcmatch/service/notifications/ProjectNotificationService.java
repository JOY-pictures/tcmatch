package com.tcmatch.tcmatch.service.notifications;

import com.tcmatch.tcmatch.events.NewProjectEvent;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.SubscriptionService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectNotificationService {

    // 🔥 Инжектируем зависимости
    private final NotificationService notificationService; // Для сохранения и пуша
    private final UserService userService;                   // Для получения списка фрилансеров
    private final SubscriptionService subscriptionService;   // Для проверки тарифа
    private final ProjectService projectService;             // (Опционально: если потребуется)

    /**
     * 🔥 ГЛАВНЫЙ СЛУШАТЕЛЬ: ОБРАБОТКА СОБЫТИЯ О НОВОМ ПРОЕКТЕ
     */
    @Async
    @EventListener
    public void handleNewProject(NewProjectEvent event) {
        try {
            ProjectDto project = event.getProjectDto();

            // 🔥 НЕ УВЕДОМЛЯЕМ СОЗДАТЕЛЯ ПРОЕКТА
            Long creatorChatId = event.getCreatorChatId();

            // 🔥 ПОЛУЧАЕМ ВСЕХ ФРИЛАНСЕРОВ
            List<UserDto> allFreelancers = userService.getAllFreelancers();

            for (UserDto freelancer : allFreelancers) {
                // Пропускаем создателя проекта
                if (freelancer.getChatId().equals(creatorChatId)) {
                    continue;
                }

                // 🔥 ПРОВЕРЯЕМ ТАРИФ ПОЛЬЗОВАТЕЛЯ
                SubscriptionTier userPlan = subscriptionService.getVerifiedSubscriptionTier(freelancer.getChatId());

                if (userPlan.isHasInstantNotifications()) {
                    // 🔥 PRO и UNLIMITED - мгновенно
                    log.info("🚀 Мгновенное уведомление для {} (тариф: {})",
                            freelancer.getChatId(), userPlan.name());
                    sendProjectNotification(freelancer.getChatId(), project);
                } else {
                    // 🔥 FREE и BASIC - с задержкой (например, 30 минут)
                    // Мы не используем Thread.sleep() напрямую
                    log.info("⏰ Отложенное уведомление для {} (тариф: {})",
                            freelancer.getChatId(), userPlan.name());
                    // 30 минут задержки
                    scheduleDelayedNotification(freelancer.getChatId(), project, 1L);
                }
            }

            log.info("✅ Уведомления о новом проекте отправлены {} фрилансерам", allFreelancers.size());

        } catch (Exception e) {
            log.error("❌ Ошибка уведомления о новом проекте: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 ВСПОМОГАТЕЛЬНЫЙ МЕТОД: Форматирование текста уведомления о проекте
     */
    private String formatProjectNotificationText(ProjectDto project) {
        return String.format(
                "🚀 <b>НОВЫЙ ПРОЕКТ НА ПЛАТФОРМЕ!</b>\n\n" +
                        "<blockquote>🎯 <b>%s</b>\n" +
                        "💰 <b>Бюджет:</b> %.0f руб\n" +
                        "⏱️ <b>Срок:</b> %d дней\n" +
                        "🛠️ <b>Навыки:</b> %s</blockquote>\n\n" +
                        "<i>💡 Успейте откликнуться первым!</i>",
                escapeHtml(project.getTitle()),
                project.getBudget(),
                project.getEstimatedDays(),
                project.getRequiredSkills() != null ?
                        escapeHtml(project.getRequiredSkills()) : "не указаны"
        );
    }

    /**
     * 🔥 МЕТОД: ОТЛОЖЕННОЕ УВЕДОМЛЕНИЕ ДЛЯ БАЗОВОГО ТАРИФА
     */
    @Async
    public void scheduleDelayedNotification(Long freelancerChatId, ProjectDto project, Long delayMinutes) {
        try {
            // 🔥 НЕ ИСПОЛЬЗУЕМ Thread.sleep() - используем CompletableFuture.delayedExecutor
            CompletableFuture.delayedExecutor(delayMinutes, TimeUnit.MINUTES)
                    .execute(() -> {
                        try {
                            // 🔥 ОТПРАВЛЯЕМ УВЕДОМЛЕНИЕ
                            sendProjectNotification(freelancerChatId, project);

                            log.info("✅ Отложенное уведомление отправлено пользователю {}", freelancerChatId);

                        } catch (Exception e) {
                            log.error("❌ Ошибка в отложенном уведомлении для пользователя {}: {}",
                                    freelancerChatId, e.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("❌ Ошибка планирования отложенного уведомления для пользователя {}: {}",
                    freelancerChatId, e.getMessage());
        }
    }

    /**
     * 🔥 МЕТОД: ОТПРАВКА УВЕДОМЛЕНИЯ О ПРОЕКТЕ
     */
    @Async
    public void sendProjectNotification(Long freelancerChatId, ProjectDto project) {
        try {
            String text = formatProjectNotificationText(project);
            String callbackData = "project:details:" + project.getId();

            // Используем центральный сервис для сохранения и пуша
            notificationService.createNotification(freelancerChatId, text, callbackData);

        } catch (Exception e) {
            log.error("❌ Ошибка отправки уведомления о проекте пользователю {}: {}",
                    freelancerChatId, e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

