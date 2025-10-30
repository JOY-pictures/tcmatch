package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserProfileData;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.NavigationService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
public class UserProfileHandler extends BaseHandler {
    private final UserService userService;

    public UserProfileHandler(KeyboardFactory keyboardFactory, NavigationService navigationService, UserService userService) {
        super(keyboardFactory, navigationService);
        this.userService = userService;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "user_profile".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        UserProfileData data = new UserProfileData(chatId, messageId, userName);

        switch (action) {
            case "show":
                showUserProfile(data);
                break;
            case "statistics":
                showStatistics(data);
                break;
            case "edit":
                showEditProfile(data);
                break;
            default:
                log.warn("❌ Unknown user_profile action: {}", action);
        }
    }

    public void showUserProfile(UserProfileData data) {
        if (!userService.hasFullAccess(data.getChatId())) {
            String message = "❌ Для доступа к профилю завершите регистрацию";
            InlineKeyboardMarkup keyboard = keyboardFactory.getKeyboardForUser(data.getChatId());
            editMessage(data.getChatId(), data.getMessageId(), message, keyboard);
            return;
        }

        User user = userService.findByChatId(data.getChatId()).orElseThrow();
        Map<String, Object> stats = userService.getUserStatistics(data.getChatId());

        // 🔥 НОВОЕ ОТОБРАЖЕНИЕ РЕЙТИНГА
        String ratingDisplay = getRatingDisplay(user);
        String verificationStatus = getVerificationStatus(user);
        String reviewStatus = getReviewStatus(user);

        String profileText = """
            👤 **ЛИЧНЫЙ КАБИНЕТ**
            
            📝 *Основная информация:*
            • Имя: %s %s
            • Username: @%s
            • Роль: %s
            • Специализация: %s
            • Уровень: %s
            %s%s
            🏆 *Профессиональный Рейтинг:*
            %s
            """.formatted(
                user.getFirstname(),
                user.getLastname() != null ? user.getLastname() : "",
                user.getUsername() != null ? user.getUsername() : "не указан",
                getRoleDisplay(user.getRole()),
                user.getSpecialization() != null ? user.getSpecialization() : "не указана",
                user.getExperienceLevel() != null ? user.getExperienceLevel() : "не указан",
                verificationStatus,
                reviewStatus,
                ratingDisplay);

        InlineKeyboardMarkup keyboard = keyboardFactory.createPersonalAccountKeyboard();
        editMessage(data.getChatId(), data.getMessageId(), profileText, keyboard);
    }

    public void showStatistics(UserProfileData data) {
        User user = userService.findByChatId(data.getChatId()).orElseThrow();

        // Временная заглушка для статистики

        Map<String, Object> stats = userService.getUserStatistics(data.getChatId());

        String statsText = """
            📊 **ДЕТАЛЬНАЯ СТАТИСТИКА**
            • Всего проектов: %d
            • Завершено: %d
            • Успешных: %d (%.1f%%)
            • В срок: %d (%.1f%%)
            • Текущих заказов: %d
            • Активных откликов: %d
            
            • В системе с: %s
            """.formatted(
                stats.get("totalProjects"),
                stats.get("completedProjects"),
                stats.get("successfulProjects"),
                user.getSuccessRate(),
                user.getOnTimeProjectsCount(),
                user.getTimelinessRate(),
                stats.get("activeOrders"),
                stats.get("activeApplications"),
                user.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        editMessage(data.getChatId(), data.getMessageId(), statsText, keyboardFactory.createBackButton());
    }

    public void showEditProfile(UserProfileData data) {
        String editText = """
                ✏️ **РЕДАКТИРОВАНИЕ ПРОФИЛЯ**
                
                🚧 Функция в разработке
                
                Скоро вы сможете:
                • Изменить специализацию
                • Добавить описание и навыки
                • Настроить уведомления
                """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createBackButton();
        editMessage(data.getChatId(), data.getMessageId(), editText, keyboard);
    }

    private String getRatingDisplay(User user) {
        double rating = user.getProfessionalRating();

        if (rating >= 1000) return "🏅 ЭЛИТА • " + rating + " ПРП";
        if (rating >= 500) return "⭐ ПРОФИ • " + rating + " ПРП";
        if (rating >= 200) return "🔥 ОПЫТНЫЙ • " + rating + " ПРП";
        if (rating >= 50) return "🚀 НАДЕЖНЫЙ • " + rating + " ПРП";
        if (rating >= 10) return "🌱 НАЧИНАЮЩИЙ • " + rating + " ПРП";
        return "🆕 НОВИЧОК • " + rating + " ПРП";
    }

    private String getVerificationStatus(User user) {
        if (user.getIsVerified()) {
            return "• ✅ Верифицирован (" + user.getVerificationMethod() + ")\\n";
        }
        return "• ⚠️ Не верифицирован\n";
    }

    private String getReviewStatus (User user) {
        if (user.getIsUnderReview()) {
            return "• 🔍 На проверке до " +
                    user.getReviewUntil().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\\n";
        }
        return "";
    }

    private String getRoleDisplay(UserRole role) {
        return switch (role) {
            case FREELANCER -> "👨‍💻 Исполнитель";
            case CUSTOMER -> "👔 Заказчик";
            case ADMIN -> "⚡ Администратор";
            default -> "👤 Пользователь";
        };
    }
}
