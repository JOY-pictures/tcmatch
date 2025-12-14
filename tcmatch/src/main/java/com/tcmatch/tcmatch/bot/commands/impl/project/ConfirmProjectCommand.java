package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import com.tcmatch.tcmatch.service.ProjectCreationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfirmProjectCommand implements Command {

    private final BotExecutor botExecutor;
    private final ProjectCreationService projectCreationService;
    private final ProjectService projectService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "confirm".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
            ProjectCreationState state = projectCreationService.getCurrentState(chatId);

            if (state == null) return;

            if (!state.isCompleted()) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Заполните все поля проекта", 5);
                return;
            }

            // 🔥 СОЗДАЕМ ПРОЕКТ В БАЗЕ ДАННЫХ
            Project project = projectService.createProject(
                    chatId,
                    state.getTitle(),
                    state.getDescription(),
                    state.getBudget(),
                    state.getRequiredSkills(),
                    state.getEstimatedDays()
            );

            projectCreationService.completeCreation(chatId);

            String successText = """
            <b>✅ ПРОЕКТ СОЗДАН!</b>

            <blockquote><b>🎯 Название:</b> %s
            <b>💰 Бюджет:</b> <code>%.0f руб</code>
            <b>⏱️ Срок:</b> <code>%d дней</code>
            <b>🛠️ Навыки:</b> %s</blockquote>

            <b>🚀 Проект теперь доступен исполнителям</b>
            <i>💡 Вы можете управлять проектом в разделе "Мои проекты"</i>
            """.formatted(
                    project.getTitle(),
                    project.getBudget(),
                    project.getEstimatedDays(),
                    project.getRequiredSkills(
                    )
            );

            userSessionService.resetToMain(chatId);

            botExecutor.editMessageWithHtml(chatId, messageId, successText,
                    commonKeyboards.createToMainMenuKeyboard());

            log.info("✅ Пользователь {} создал проект {}", chatId, project.getId());

        } catch (Exception e) {
            log.error("❌ Ошибка создания проекта: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка создания проекта: " + e.getMessage(), 5);
        }
    }
}