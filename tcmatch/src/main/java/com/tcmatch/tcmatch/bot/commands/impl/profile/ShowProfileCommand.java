package com.tcmatch.tcmatch.bot.commands.impl.profile;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProfileKeyboards;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowProfileCommand implements Command {

    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final ProfileKeyboards profileKeyboards;
    private final BotExecutor botExecutor;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "user_profile".equals(actionType) && "show".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        if (!userService.hasFullAccess(context.getChatId())) {
            String message = "<b>❌ Для доступа к профилю завершите регистрацию</b>";
            InlineKeyboardMarkup keyboard = commonKeyboards.getKeyboardForUser(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), message, keyboard);
            return;
        }

        UserDto user = userService.getUserDtoByChatId(context.getChatId()).orElseThrow();
        Map<String, Object> stats = userService.getUserStatistics(context.getChatId());

        String ratingDisplay = getRatingDisplay(user);
        String verificationStatus = getVerificationStatus(user);
        String reviewStatus = getReviewStatus(user);

        String profileText = """
            <b>👤 **ЛИЧНЫЙ КАБИНЕТ**</b>
            
            <i>📝 *Основная информация:*
            • Имя: %s %s
            • Username: @%s
            • Роль: %s
            • Специализация: %s
            • Уровень: %s
            %s%s
            🏆 *Профессиональный Рейтинг:*
            %s</i>
            """.formatted(
                user.getDisplayName(),
                user.getLastName() != null ? user.getLastName() : "",
                user.getUserName() != null ? user.getUserName() : "не указан",
                getRoleDisplay(user.getRole()),
                user.getSpecialization() != null ? user.getSpecialization() : "не указана",
                user.getExperienceLevel() != null ? user.getExperienceLevel() : "не указан",
                verificationStatus,
                reviewStatus,
                ratingDisplay);

        InlineKeyboardMarkup keyboard = profileKeyboards.createPersonalAccountKeyboard(context.getChatId());
        botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), profileText, keyboard);
    }

    private String getRatingDisplay(UserDto user) {
        double rating = user.getProfessionalRating();

        if (rating >= 1000) return "🏅 ЭЛИТА • " + rating + " ПРП";
        if (rating >= 500) return "⭐ ПРОФИ • " + rating + " ПРП";
        if (rating >= 200) return "🔥 ОПЫТНЫЙ • " + rating + " ПРП";
        if (rating >= 50) return "🚀 НАДЕЖНЫЙ • " + rating + " ПРП";
        if (rating >= 10) return "🌱 НАЧИНАЮЩИЙ • " + rating + " ПРП";
        return "🆕 НОВИЧОК • " + rating + " ПРП";
    }

    private String getVerificationStatus(UserDto user) {
        if (user.getIsVerified()) {
            return "• ✅ Верифицирован";
        }
        return "• ⚠️ Не верифицирован\n";
    }

    private String getReviewStatus(UserDto user) {
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
