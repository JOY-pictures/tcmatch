package com.tcmatch.tcmatch.bot.commands.impl.profile;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProfileKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowCustomerProfileCommand implements Command {

    private final BotExecutor botExecutor;
    private final ProjectService projectService;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final ProfileKeyboards profileKeyboards;
    @Override
    public boolean canHandle(String actionType, String action) {
        return "profile".equals(actionType) && "show_customer".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Long customerChatId = Long.parseLong(context.getParameter());

            // 🔥 ПОЛУЧАЕМ ИНФОРМАЦИЮ О ЗАКАЗЧИКЕ
            UserDto customer = userService.getUserDtoByChatId(customerChatId)
                    .orElseThrow(() -> new RuntimeException("Заказчик не найден"));

            // 🔥 ПРОВЕРЯЕМ ЧТО ЭТО ЗАКАЗЧИК
            if (customer.getRole() != UserRole.CUSTOMER) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Этот пользователь не является заказчиком", 5);
                return;
            }

            // 🔥 ПОЛУЧАЕМ ПРОЕКТЫ ЗАКАЗЧИКА
            List<Long> customerProjectsIds = projectService.getUserProjectIds(customerChatId);
            List<ProjectDto> customerProjects = projectService.getProjectsByIds(customerProjectsIds);

            // 🔥 ПОЛУЧАЕМ СТАТИСТИКУ ДЛЯ ЗАКАЗЧИКА
            CustomerStats stats = getCustomerStats(customerProjects, customer);

            String profileText = formatCustomerProfile(customer, stats, customerProjects);
            InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            botExecutor.editMessageWithHtml(chatId, mainMessageId, profileText, keyboard);

            log.info("✅ Показан профиль заказчика {} для пользователя {}", customerChatId, chatId);

        } catch (Exception e) {
            log.error("❌ Ошибка показа профиля заказчика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка загрузки профиля заказчика: " + e.getMessage(), 5);
        }
    }

    private CustomerStats getCustomerStats(List<ProjectDto> customerProjects, UserDto customer) {
        long activeProjects = customerProjects.stream()
                .filter(p -> p.getStatus() == UserRole.ProjectStatus.OPEN)
                .count();

        long completedProjects = customerProjects.stream()
                .filter(p -> p.getStatus() == UserRole.ProjectStatus.COMPLETED)
                .count();

        long totalProjects = customerProjects.size();

        // 🔥 СТАТИСТИКА БЮДЖЕТОВ
        double totalBudget = customerProjects.stream()
                .mapToDouble(ProjectDto::getBudget)
                .sum();

        double averageBudget = totalProjects > 0 ? totalBudget / totalProjects : 0.0;

        // 🔥 СТАТИСТИКА ОТКЛИКОВ
        int totalApplications = customerProjects.stream()
                .mapToInt(ProjectDto::getApplicationsCount)
                .sum();

        double avgApplicationsPerProject = totalProjects > 0 ? (double) totalApplications / totalProjects : 0.0;

        return new CustomerStats(
                activeProjects, completedProjects, totalProjects,
                totalBudget, averageBudget,
                totalApplications, avgApplicationsPerProject,
                customer.getProfessionalRating(),
                customer.getSuccessRate(),
                customer.getTimelinessRate(),
                customer.getIsVerified()
        );
    }

    private String formatCustomerProfile(UserDto customer, CustomerStats stats, List<ProjectDto> projects) {
        String displayName = customer.getDisplayName();
        String verificationStatus = customer.getVerificationInfo();
        String registrationDate = formatRegistrationDate(customer.getRegisteredAt());
        String memberSince = calculateMemberSince(customer.getRegisteredAt());
        String activityStatus = customer.getActivityStatus();

        return """
        <b>👔 ПРОФИЛЬ ЗАКАЗЧИКА</b>

        <blockquote>📛 <b>Имя:</b> %s
        🆔 <b>Username:</b> @%s
        📅 <b>Регистрация:</b> %s
        🕐 <b>На платформе:</b> %s
        📊 <b>Статус:</b> %s
        🔄 <b>Активность:</b> %s</blockquote>

        <b>⭐ Рейтинг заказчика:</b>
        <blockquote>🏆 <b>Проф. рейтинг:</b> %s
        📈 <b>Успешных проектов:</b> %.1f%%
        ⏱️ <b>Своевременных:</b> %.1f%%</blockquote>

        <b>📊 Статистика проектов:</b>
        <blockquote>📈 <b>Всего проектов:</b> %d
        🟢 <b>Активных:</b> %d
        ✅ <b>Завершенных:</b> %d
        💰 <b>Общий бюджет:</b> %.0f руб
        📊 <b>Средний бюджет:</b> %.0f руб
        📨 <b>Всего откликов:</b> %d
        📝 <b>Откликов/проект:</b> %.1f</blockquote>

        <b>💼 Активные проекты:</b>
        %s

        <b>💡 О заказчике:</b>
        <i>%s</i>
        """.formatted(
                escapeHtml(displayName),
                escapeHtml(customer.getUserName() != null ? customer.getUserName() : "Не указан"),
                registrationDate,
                memberSince,
                verificationStatus,
                activityStatus,
                formatProfessionalRating(customer.getProfessionalRating()),
                customer.calculateSuccessPercentage(),
                customer.calculateTimelinessPercentage(),
                stats.totalProjects(),
                stats.activeProjects(),
                stats.completedProjects(),
                stats.totalBudget(),
                stats.averageBudget(),
                stats.totalApplications(),
                stats.avgApplicationsPerProject(),
                getActiveProjectsPreview(customer.getChatId(), projects),
                getCustomerAdditionalInfo(customer)
        );
    }

    private String getActiveProjectsPreview(Long customerChatId, List<ProjectDto> projects) {
        List<ProjectDto> activeProjects = projects
                .stream()
                .filter(p -> p.getStatus() == UserRole.ProjectStatus.OPEN)
                .limit(3)
                .toList();

        if (activeProjects.isEmpty()) {
            return "<i>Нет активных проектов</i>";
        }

        StringBuilder sb = new StringBuilder();
        for (ProjectDto project : activeProjects) {
            String projectTitle = escapeHtml(project.getTitle().length() > 25 ?
                    project.getTitle().substring(0, 25) + "..." : project.getTitle());

            sb.append(String.format("• 🟢 %s (%.0f руб | %d откликов)\n",
                    projectTitle,
                    project.getBudget(),
                    project.getApplicationsCount()));
        }
        return sb.toString();
    }

    private String getCustomerAdditionalInfo(UserDto customer) {
        StringBuilder info = new StringBuilder();

        if (customer.getSpecialization() != null) {
            info.append("Интересуется: ").append(customer.getSpecialization()).append("\\n");
        }

        if (customer.getSkills() != null) {
            String skillsPreview = customer.getSkills().length() > 80 ?
                    customer.getSkills().substring(0, 80) + "..." : customer.getSkills();
            info.append("Технологии: ").append(skillsPreview).append("\\n");
        }

        if (customer.getCompletedProjectsCount() != null && customer.getCompletedProjectsCount() > 0) {
            info.append("Успешных сделок: ").append(customer.getCompletedProjectsCount()).append("\\n");
        }

        if (info.length() == 0) {
            return "Заказчик пока не добавил информацию о себе";
        }

        return info.toString();
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

    // 🔥 DTO для статистики заказчика
    private record CustomerStats(
            long activeProjects,
            long completedProjects,
            long totalProjects,
            double totalBudget,
            double averageBudget,
            int totalApplications,
            double avgApplicationsPerProject,
            Double professionalRating,
            Double successRate,
            Double timelinessRate,
            Boolean isVerified
    ) {}
}