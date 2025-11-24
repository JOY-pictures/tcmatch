package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfirmWithdrawProjectCommand implements Command {

    private final ProjectService projectService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "confirm_withdraw".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
        String[] projectContext = context.getParameter().split(":");
        Long projectId = Long.parseLong(projectContext[0]);
        try {
            ProjectDto project = projectService.getProjectDtoById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));

            if (!project.getCustomerChatId().equals(chatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ У вас нет доступа к этому проекту", 5);
                return;
            }

            // 🔥 ПРОВЕРЯЕМ, ЧТО ПРОЕКТ МОЖНО УДАЛИТЬ
            if (!canWithdrawProject(project.getStatus())) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Нельзя удалить проект со статусом: " + getProjectStatusDisplay(project.getStatus()), 5);
                return;
            }
            String warningText = """
            <b>⚠️ **ПОДТВЕРЖДЕНИЕ УДАЛЕНИЯ ПРОЕКТА**</b>
            
            <blockquote>📋 <b>Проект:</b> %s
            💰 <b>Бюджет:</b> %.0f руб
            ⏱️ <b>Срок:</b> %d дней
            📅 <b>Создан:</b> %s
            👀 <b>Просмотров:</b> %d
            📨 <b>Откликов:</b> %d</blockquote>
            
            🔴<b> Внимание!</b> После удаления:
            <i>• Проект будет перемещен в архив
            • Все отклики на проект будут аннулированы
            • Исполнители получат уведомления об отмене
            • Вернуть проект будет невозможно
            • Статистика проекта будет утеряна</i>
            
            ⚠️ <b>Особые случаи:</b>
            <i>%s</i>
            
            ❓ <b>Вы точно хотите удалить этот проект?</b>
            """.formatted(
                    escapeHtml(project.getTitle()),
                    project.getBudget(),
                    project.getEstimatedDays(),
                    project.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    project.getViewsCount() != null ? project.getViewsCount() : 0,
                    project.getApplicationsCount() != null ? project.getApplicationsCount() : 0,
                    getSpecialCasesWarning(project)
            );

            InlineKeyboardMarkup keyboard = projectKeyboards.createProjectWithdrawConfirmationKeyboard(projectId);

            botExecutor.editMessageWithHtml(chatId, messageId, warningText, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка подтверждения удаления проекта: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка подтверждения удаления", 5);
        }
    }

    private String getSpecialCasesWarning(ProjectDto projectDto) {
        StringBuilder warning = new StringBuilder();

        if (projectDto.getApplicationsCount() != null && projectDto.getApplicationsCount() > 0) {
            warning.append("• ").append(projectDto.getApplicationsCount())
                    .append(" исполнителей уже откликнулись на проект\\n");
        }

        if (projectDto.getStatus() == UserRole.ProjectStatus.IN_PROGRESS) {
            warning.append("• Проект уже в работе, удаление может повлиять на репутацию\\n");
        }

        if (projectDto.getViewsCount() != null && projectDto.getViewsCount() > 10) {
            warning.append("• Проект получил ").append(projectDto.getViewsCount())
                    .append(" просмотров, удаление может разочаровать потенциальных исполнителей\\n");
        }

        if (warning.length() == 0) {
            return "Проект можно безопасно удалить";
        }

        return warning.toString();
    }


    private boolean canWithdrawProject(UserRole.ProjectStatus projectStatus) {
        // 🔥 ПРОЕКТ МОЖНО УДАЛИТЬ ТОЛЬКО В ОПРЕДЕЛЕННЫХ СТАТУСАХ
        return switch (projectStatus) {
            case OPEN -> true;
            case IN_PROGRESS, COMPLETED, CANCELLED, UNDER_REVIEW, DISPUTE -> false;
        };
    }

    private String getProjectStatusDisplay(UserRole.ProjectStatus projectStatus) {
        return switch (projectStatus) {
            case OPEN -> "🟢 Активен";
            case UNDER_REVIEW -> "⚪ Ожидает модерации";
            case IN_PROGRESS -> "🟡 В работе";
            case COMPLETED -> "✅ Завершен";
            case CANCELLED -> "🔴 Отменен";
            case DISPUTE -> "В ожидании";
        };
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
