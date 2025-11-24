package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class FavoriteProjectCommand implements Command {

    private final UserService userService;
    private final BotExecutor botExecutor;
    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final ProjectViewService projectViewService;
    private final RoleBasedMenuService roleBasedMenuService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "favorite".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        // Parameter format: "add:123" или "remove:456"
        String[] parts = context.getParameter().split(":");

        if (parts.length < 2) {
            log.warn("❌ Некорректный параметр для избранного: {}", context.getParameter());
            return;
        }

        String actionType = parts[0]; // "add" или "remove"
        Long projectId;

        try {
            projectId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            log.error("❌ Некорректный ID проекта '{}' для избранного у пользователя {}", parts[1], chatId);
            // Отправка уведомления пользователю
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Произошла ошибка с ID проекта.", 5);
            return;
        }

        try {
            if ("add".equals(actionType)) {
                userService.addFavoriteProject(chatId, projectId);
                log.warn("Пользователь {} добавил в избранное проект {}", chatId, projectId);
            } else if ("remove".equals(actionType)) {
                userService.removeFavoriteProject(chatId, projectId);
                log.warn("Пользователь {} удалил из избранного проект {}", chatId, projectId);
            } else {
                log.warn("❌ Неизвестный тип действия для избранного: {}", actionType);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при изменении избранного для {} ({}): {}", chatId, projectId, e.getMessage());
        }

        // 1. УВЕДОМЛЕНИЕ: Отправляем временное уведомление (используем ваш существующий метод)

        // 2. ОБНОВЛЕНИЕ UI: Перерисовываем детальную карточку проекта.
        // Устанавливаем в data.parameter ID проекта, чтобы showProjectDetail(data) знал, какой проект загрузить.
        context.setParameter(String.valueOf(projectId));

        // Поскольку мы только что обновили статус избранного,
        // нам нужно, чтобы карточка детализации обновилась (самый надежный способ - повторный вызов).
        // 🔥 Важно: showProjectDetail должен использовать messageId из data для редактирования.
        showProjectDetail(context);
    }

    public void showProjectDetail(CommandContext context) {
        Long chatId = context.getChatId();

        try {
            Long projectId;
            String parameter = context.getParameter();

            // 🔥 ПРОВЕРЯЕМ - ПЕРЕДАН ID ПРОЕКТА ИЛИ ID ОТКЛИКА?
            if (parameter.startsWith("app_")) {
                // 🔥 ЕСЛИ ПЕРЕДАН ID ОТКЛИКА (app_123) - ПОЛУЧАЕМ ID ПРОЕКТА
                Long applicationId = Long.parseLong(parameter.replace("app_", ""));
                projectId = applicationService.getProjectIdByApplicationId(applicationId);
            } else {
                // 🔥 ЕСЛИ ПЕРЕДАН ОБЫЧНЫЙ ID ПРОЕКТА
                projectId = Long.parseLong(parameter);
            }

            ProjectDto project = projectService.getProjectDtoById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            botExecutor.deletePreviousMessages(chatId);

            // 🔥 РЕГИСТРИРУЕМ ПРОСМОТР ТОЛЬКО ЗДЕСЬ - КОГДА ПОЛЬЗОВАТЕЛЬ ДЕЙСТВИТЕЛЬНО СМОТРИТ ПРОЕКТ
            projectViewService.registerProjectView(chatId, projectId);

            String projectText = formatProjectDetails(project);

            boolean canApply = roleBasedMenuService.canUserApplyToProjects(chatId) &&
                    !roleBasedMenuService.isProjectOwner(chatId, project.getCustomerChatId());

            InlineKeyboardMarkup keyboard = projectKeyboards.createProjectDetailsKeyboard(
                    chatId, projectId, canApply);

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);

            botExecutor.editMessageWithHtml(chatId, mainMessageId, projectText, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка показа деталей проекта: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки информации о проекте", 5);
        }
    }

    private String formatProjectDetails(ProjectDto project) {
        return """
            <b>💼 **ДЕТАЛИ ПРОЕКТА**</b>

            <blockquote><b>🎯 *Название:*</b> %s
            <b>💰 *Бюджет:*</b> %.0f руб
            <b>⏱️ *Срок:*</b> %d дней
            <b>👀 *Просмотров:*</b> %d
            <b>📨 *Откликов:*</b> %d

            <b>📝 *Описание:*</b>
            <i>%s</i>

            <b>🛠️ *Требуемые навыки:*</b>
            <u>%s</u></blockquote>

            <b>👔 *Заказчик:*</b> @%s
            <b>📊 *Рейтинг заказчика:*</b> ⭐ %.1f/5.0
            """.formatted(
                project.getTitle(),
                project.getBudget(),
                project.getEstimatedDays(),
                project.getViewsCount(),
                project.getApplicationsCount(),
                project.getDescription(),
                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны",
                project.getCustomerUserName() != null ? project.getCustomerUserName() : "скрыт",
                project.getCustomerRating()
        );
    }
}