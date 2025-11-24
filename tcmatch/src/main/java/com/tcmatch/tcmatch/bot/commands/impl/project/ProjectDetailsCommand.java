package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.ProjectViewService;
import com.tcmatch.tcmatch.service.RoleBasedMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectDetailsCommand implements Command {

    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final BotExecutor botExecutor;
    private final ProjectViewService projectViewService;
    private final RoleBasedMenuService roleBasedMenuService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "details".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
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
            <b>⏱️ *Предполагаемый срок:*</b> %d дней
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
