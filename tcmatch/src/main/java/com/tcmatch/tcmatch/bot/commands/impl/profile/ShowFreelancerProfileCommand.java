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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowFreelancerProfileCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final ProfileKeyboards profileKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "profile".equals(actionType) && "show_freelancer".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Long freelancerChatId = Long.parseLong(context.getParameter());

            // 🔥 ПОЛУЧАЕМ ИНФОРМАЦИЮ О ФРИЛАНСЕРЕ
            UserDto freelancer = userService.getUserDtoByChatId(freelancerChatId)
                    .orElseThrow(() -> new RuntimeException("Фрилансер не найден"));

            // 🔥 ПРОВЕРЯЕМ ЧТО ЭТО ФРИЛАНСЕР
            if (freelancer.getRole() != UserRole.FREELANCER) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Этот пользователь не является фрилансером", 5);
                return;
            }

            String profileText = formatFreelancerProfile(freelancer);
            InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            botExecutor.editMessageWithHtml(chatId, mainMessageId, profileText, keyboard);

            log.info("✅ Показан профиль фрилансера {} для пользователя {}", freelancerChatId, chatId);

        } catch (Exception e) {
            log.error("❌ Ошибка показа профиля фрилансера: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка загрузки профиля фрилансера: " + e.getMessage(), 5);
        }
    }

    private String formatFreelancerProfile(UserDto freelancer) {
        String displayName = freelancer.getDisplayName();
        String verificationStatus = getVerificationStatus(freelancer);
        String registrationDate = formatRegistrationDate(freelancer.getRegisteredAt());
        String memberSince = calculateMemberSince(freelancer.getRegisteredAt());

        return """
            <b>💻 ПРОФИЛЬ ФРИЛАНСЕРА</b>

            <blockquote>📛 <b>Имя:</b> %s
            🆔 <b>Username:</b> @%s
            📅 <b>Регистрация:</b> %s
            🕐 <b>На платформе:</b> %s
            📊 <b>Статус:</b> %s</blockquote>

            <b>⭐ Система репутации (ПРП):</b>
            <blockquote>🏆 <b>Проф. рейтинг:</b> %s
            📈 <b>Коэф. успешности (КУЗ):</b> %.1f%%
            ⏱️ <b>Коэф. своевременности (КС):</b> %.1f%%</blockquote>

            <b>📊 Статистика выполнения:</b>
            <blockquote>📦 <b>Всего проектов:</b> %d
            ✅ <b>Успешно завершено:</b> %d
            🎯 <b>Успешность:</b> %.1f%%
            ⏰ <b>Своевременность:</b> %.1f%%</blockquote>

            <b>🛠️ Профессиональная информация:</b>
            <blockquote>🎯 <b>Специализация:</b> %s
            📚 <b>Уровень опыта:</b> %s
            🔧 <b>Навыки:</b> %s</blockquote>

            <b>💡 О фрилансере:</b>
            <i>%s</i>

            <b>📞 Контакты:</b>
            • Доступны после согласования
            • Общение через платформу

            <b>🚀 Готов к работе:</b>
            %s
            """.formatted(
                escapeHtml(displayName),
                escapeHtml(freelancer.getUserName() != null ? freelancer.getUserName() : "Не указан"),
                registrationDate,
                memberSince,
                verificationStatus,
                formatProfessionalRating(freelancer.getProfessionalRating()),
                freelancer.getSuccessRate() != null ? freelancer.getSuccessRate() : 0.0,
                freelancer.getTimelinessRate() != null ? freelancer.getTimelinessRate() : 0.0,
                freelancer.getTotalProjectsCount() != null ? freelancer.getTotalProjectsCount() : 0,
                freelancer.getSuccessfulProjectsCount() != null ? freelancer.getSuccessfulProjectsCount() : 0,
                calculateSuccessPercentage(freelancer),
                calculateTimelinessPercentage(freelancer),
                escapeHtml(freelancer.getSpecialization() != null ? freelancer.getSpecialization() : "Не указана"),
                escapeHtml(freelancer.getExperienceLevel() != null ? freelancer.getExperienceLevel() : "Не указан"),
                escapeHtml(freelancer.getSkills() != null ? freelancer.getSkills() : "Не указаны"),
                getFreelancerBio(freelancer),
                getAvailabilityStatus(freelancer)
        );
    }

    private double calculateSuccessPercentage(UserDto freelancer) {
        if (freelancer.getTotalProjectsCount() == null || freelancer.getTotalProjectsCount() == 0) {
            return 0.0;
        }
        if (freelancer.getSuccessfulProjectsCount() == null) {
            return 0.0;
        }
        return (double) freelancer.getSuccessfulProjectsCount() / freelancer.getTotalProjectsCount() * 100;
    }

    private double calculateTimelinessPercentage(UserDto freelancer) {
        if (freelancer.getTotalProjectsCount() == null || freelancer.getTotalProjectsCount() == 0) {
            return 0.0;
        }
        if (freelancer.getOnTimeProjectsCount() == null) {
            return 0.0;
        }
        return (double) freelancer.getOnTimeProjectsCount() / freelancer.getTotalProjectsCount() * 100;
    }

    private String getFreelancerBio(UserDto freelancer) {
        if (freelancer.getSkills() != null && freelancer.getSpecialization() != null) {
            return String.format("Специализируется на %s. Ключевые навыки: %s",
                    freelancer.getSpecialization(),
                    freelancer.getSkills().length() > 100 ?
                            freelancer.getSkills().substring(0, 100) + "..." : freelancer.getSkills());
        } else if (freelancer.getSkills() != null) {
            return "Навыки: " + freelancer.getSkills();
        } else {
            return "Фрилансер пока не добавил информацию о себе";
        }
    }

    private String getAvailabilityStatus(UserDto freelancer) {
        if (freelancer.getStatus() == UserRole.UserStatus.ACTIVE) {
            return "🟢 Доступен для новых проектов";
        } else if (freelancer.getStatus() == UserRole.UserStatus.BLOCKED) {
            return "🟡 Заблокирован на платформе";
        } else {
            return "🔴 Не доступен для новых проектов";
        }
    }

    private String getVerificationStatus(UserDto user) {
        if (user.getIsVerified() != null && user.getIsVerified()) {
            return "✅ Верифицирован";
        } else {
            return "⚪ Не верифицирован";
        }
    }

    private String formatProfessionalRating(Double professionalRating) {
        if (professionalRating == null || professionalRating == 0.0) {
            return "<i>еще нет оценок</i>";
        }
        return String.format("⭐ %.1f/5.0", professionalRating);
    }

    private String formatRegistrationDate(LocalDateTime registrationDate) {
        if (registrationDate == null) {
            return "<i>неизвестно</i>";
        }
        return registrationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String calculateMemberSince(LocalDateTime registrationDate) {
        if (registrationDate == null) {
            return "<i>неизвестно</i>";
        }

        long months = java.time.temporal.ChronoUnit.MONTHS.between(registrationDate, LocalDateTime.now());
        if (months == 0) {
            return "менее месяца";
        } else if (months == 1) {
            return "1 месяц";
        } else if (months < 12) {
            return months + " месяцев";
        } else {
            long years = months / 12;
            return years + " " + getYearsText(years);
        }
    }

    private String getYearsText(long years) {
        if (years == 1) return "год";
        if (years >= 2 && years <= 4) return "года";
        return "лет";
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