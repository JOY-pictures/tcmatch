package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.NotificationKeyboards;
import com.tcmatch.tcmatch.events.ApplicationStatusChangedEvent;
import com.tcmatch.tcmatch.events.NewApplicationEvent;
import com.tcmatch.tcmatch.events.NewProjectEvent;
import com.tcmatch.tcmatch.model.Notification;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.NotificationStatus;
import com.tcmatch.tcmatch.model.enums.SubscriptionPlan;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final ProjectService projectService;
    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService; // 🔥 Используем для ID пуша
    private final NotificationKeyboards notificationKeyboards; // 🔥 Нужен для кнопки "В Центр"
    private final SubscriptionService subscriptionService;

    @Transactional
    @Async
    public void createNotification(Long userChatId, String text, String callbackData) {
        // 🔥 УДАЛЯЕМ: User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Создаем и сохраняем Entity в БД (передаем только userId)
        Notification notification = new Notification(userChatId, text, callbackData); // 🔥 ИЗМЕНЕНИЕ
        notificationRepository.save(notification);

        // ... (остальная логика)

        // 2. ЗАПУСКАЕМ ЛОГИКУ "УМНОГО ПУША"
        // Нам нужен chatId, поэтому здесь все еще требуется обращение к UserService,
        // чтобы получить chatId по userId из БД
        User user = userService.findByChatId(userChatId).orElseThrow(() -> new RuntimeException("User not found"));
        triggerSmartPush(user.getChatId());
    }

    /**
     * 🔥 ОТПРАВКА УВЕДОМЛЕНИЯ О ПРОЕКТЕ
     */
    @Async
    public void sendProjectNotification(Long freelancerChatId, ProjectDto project) {
        try {
            String text = String.format(
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

            String callbackData = "project:details:" + project.getId();

            createNotification(freelancerChatId, text, callbackData);

        } catch (Exception e) {
            log.error("❌ Ошибка отправки уведомления о проекте пользователю {}: {}",
                    freelancerChatId, e.getMessage());
        }
    }

    /**
     * 🔥 ОТЛОЖЕННОЕ УВЕДОМЛЕНИЕ ДЛЯ БАЗОВОГО ТАРИФА
     */
    @Async
    public void scheduleDelayedNotification(Long freelancerChatId, ProjectDto project, Long delayMinutes) {
        try {
            log.info("⏰ Отложенное уведомление для пользователя {} через {} минут",
                    freelancerChatId, delayMinutes);

            // 🔥 НЕ ИСПОЛЬЗУЕМ Thread.sleep() - вместо этого используем CompletableFuture.delayedExecutor
            CompletableFuture.delayedExecutor(delayMinutes, TimeUnit.MINUTES)
                    .execute(() -> {
                        try {
                            // 🔥 ПРОСТО ОТПРАВЛЯЕМ УВЕДОМЛЕНИЕ - если пользователь перешел на PRO/UNLIMITED,

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
     * 🔥 ЛОГИКА "УМНОГО ПУША" (Удаление старого + Отправка нового)
     */
    @Async
    public void triggerSmartPush(Long chatId) {

        Integer oldPushMessageId = userSessionService.getLastPushMessageId(chatId);

        // 1. Удаляем старый пуш, чтобы вызвать ПУШ (звук/вибрацию) в Telegram
        if (oldPushMessageId != null) {
            botExecutor.deleteMessage(chatId, oldPushMessageId);
        }

        // 2. Собираем текст для нового "пуша"
        String pushText = buildPushSummary(chatId);

        // 3. Отправляем НОВОЕ сообщение
        Integer newPushMessageId = botExecutor.sendHtmlMessageReturnId(
                chatId,
                pushText,
                notificationKeyboards.createGoToNotificationCenterKeyboard() // Тебе нужно будет создать эту клавиатуру
        );

        // 4. Сохраняем ID нового "пуша" в сессию
        if (newPushMessageId != null) {
            userSessionService.setLastPushMessageId(chatId, newPushMessageId);
            log.debug("New push message ID {} saved for user {}", newPushMessageId, chatId);
        }
    }

    public List<Long> getAllNotificationIds(Long userChatId) {
        // Используем новый метод репозитория
        return notificationRepository.findIdByUserIdOrderByCreatedAtDesc(userChatId);
    }

    public Notification findById(Long notificationId) {
        return notificationRepository.findById(notificationId).orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(n -> n.setStatus(NotificationStatus.READ));
    }

    /**
     * 🔥 2. Получает список уведомлений по их ID (для рендеринга страницы).
     */
    public List<Notification> getNotificationsByIds(List<Long> notificationIds) {
        // ВАЖНО: findAllById() не гарантирует порядок.
        // Если порядок важен, можно использовать сортировку в Java или более сложный SQL-запрос.
        // Для простоты используем стандартный метод:
        return notificationRepository.findAllById(notificationIds);
    }

    /**
     * 🔥 3. Очищает ID push-сообщения из сессии и удаляет его из чата.
     */
    @Transactional
    public void clearPushMessageAndSession(Long chatId) {
        // Используем метод, который ты добавил в UserSessionService
        Integer messageId = userSessionService.getLastPushMessageId(chatId);

        if (messageId != null) {
            // 1. Удаляем сообщение из чата
            try {
                // botExecutor должен быть инжектирован в NotificationService
                botExecutor.deleteMessage(chatId, messageId);
            } catch (Exception e) {
                // Игнорируем ошибку, если сообщение уже удалено пользователем
                log.warn("Could not delete push message {}. Already deleted or error: {}", messageId, e.getMessage());
            }

            // 2. Очищаем ID в сессии
            userSessionService.setLastPushMessageId(chatId, null);
            log.info("🗑️ Cleared last push message ID {} for user {}", messageId, chatId);
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
        // Или можно пометить как DELETED, если не хочешь удалять:
        // notificationRepository.findById(notificationId).ifPresent(n -> n.setStatus(NotificationStatus.DELETED));
    }

    // ... (Метод для сборки текста push-уведомления)
    private String buildPushSummary(Long chatId) {
        // Здесь нам нужен ID пользователя, а не chatId
        User user = userService.findByChatId(chatId).orElseThrow();

        // Получаем последние 3 непрочитанных
        List<Notification> unread = notificationRepository.findByUserChatIdAndStatusOrderByCreatedAtDesc(
                user.getChatId(),
                NotificationStatus.UNREAD,
                PageRequest.of(0, 3)
        );
        long totalUnread = notificationRepository.countByUserChatIdAndStatus(user.getChatId(), NotificationStatus.UNREAD);

        StringBuilder sb = new StringBuilder("🔔 **НОВЫЕ СОБЫТИЯ В ВАШЕМ АККАУНТЕ**\n\n");

        for (Notification n : unread) {
            sb.append("• ").append(n.getText()).append("\n");
        }

        if (totalUnread > unread.size()) {
            sb.append(String.format("\n... и еще <b>%d</b> непрочитанных.", totalUnread - unread.size()));
        }

        return sb.toString();
    }

    /**
     * 🔥 МЕТОД "НАБЛЮДАТЕЛЯ" (@EventListener)
     * Он просто преобразует событие в Entity и вызывает createNotification.
     */
    @Async
    @EventListener
    public void handleApplicationStatusChange(ApplicationStatusChangedEvent event) {
        try {
            // ... (логика из прошлого шага)
            String text;
            String callbackData = "application:details:" + event.getApplicationDto().getId();

            if (event.getNewStatus() == UserRole.ApplicationStatus.ACCEPTED) {
                text = String.format("Ваш отклик на проект «%s» был ПРИНЯТ!", event.getApplicationDto().getProject().getTitle());
            } else if (event.getNewStatus() == UserRole.ApplicationStatus.REJECTED) {
                text = String.format("Ваш отклик на проект «%s» был ОТКЛОНЕН.", event.getApplicationDto().getProject().getTitle());
            } else {
                return;
            }

            // Сохраняем и вызываем "Умный пуш"
            createNotification(event.getApplicationDto().getFreelancer().getChatId(), text, callbackData);

        } catch (Exception e) {
            log.error("❌ Ошибка обработки события отклика: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 УВЕДОМЛЕНИЕ ЗАКАЗЧИКУ О НОВОМ ОТКЛИКЕ
     */
    @Async
    @EventListener
    public void handleNewApplication(NewApplicationEvent event) {
        try {
            ApplicationDto application = event.getApplicationDto();

            String text = String.format(
                    "📨 <b>Новый отклик</b> на проект <i>«%s»</i>\n\n" +
                            "👤 Исполнитель: %s\n" +
                            "💰 Предложил: %.0f руб\n" +
                            "⏱️ Срок: %d дней",
                    application.getProject().getTitle(),
                    application.getFreelancer().getDisplayName() != null ?
                            application.getFreelancer().getDisplayName() : "Аноним",
                    application.getProposedBudget(),
                    application.getProposedDays()
            );

            String callbackData = "application:details:" + application.getId();

            // 🔥 Отправляем уведомление заказчику
            createNotification(
                    application.getProject().getCustomerChatId(),
                    text,
                    callbackData
            );

            log.info("✅ Уведомление отправлено заказчику {} о новом отклике",
                    application.getProject().getCustomerChatId());

        } catch (Exception e) {
            log.error("❌ Ошибка уведомления заказчика о новом отклике: {}", e.getMessage(), e);
        }
    }

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
                SubscriptionPlan userPlan = subscriptionService.getUserSubscriptionPlan(freelancer.getChatId());

                if (userPlan.hasInstantNotifications()) {
                    // 🔥 PRO и UNLIMITED - мгновенно
                    log.info("🚀 Мгновенное уведомление для {} (тариф: {})",
                            freelancer.getChatId(), userPlan.name());
                    sendProjectNotification(freelancer.getChatId(), project);
                } else {
                    // 🔥 FREE и BASIC - с задержкой
                    log.info("⏰ Отложенное уведомление для {} (тариф: {})",
                            freelancer.getChatId(), userPlan.name());
                    scheduleDelayedNotification(freelancer.getChatId(), project, 0L);
                }
            }

            log.info("✅ Уведомления о новом проекте отправлены {} фрилансерам", allFreelancers.size());

        } catch (Exception e) {
            log.error("❌ Ошибка уведомления о новом проекте: {}", e.getMessage(), e);
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
