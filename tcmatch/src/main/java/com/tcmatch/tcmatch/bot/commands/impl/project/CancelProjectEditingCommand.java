package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import com.tcmatch.tcmatch.service.ProjectCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class CancelProjectEditingCommand implements Command {

    private final BotExecutor botExecutor;
    private final ProjectCreationService projectCreationService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "edit_cancel".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
            ProjectCreationState state = projectCreationService.getCurrentState(chatId);
            if (state == null) return;

            // 🔥 ВОЗВРАЩАЕМСЯ В РЕЖИМ ПОДТВЕРЖДЕНИЯ
            state.finishEditing();
            projectCreationService.updateCurrentState(chatId, state);

            showCurrentStep(chatId, messageId);

        } catch (Exception e) {
            log.error("❌ Ошибка отмены редактирования: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка отмены редактирования", 5);
        }
    }

    private void showCurrentStep(Long chatId, Integer messageId) {
        ProjectCreationState state = projectCreationService.getCurrentState(chatId);
        if (state == null) return;

        String text = formatProjectConfirmation(state);
        InlineKeyboardMarkup keyboard = projectKeyboards.createProjectConfirmationKeyboard();

        botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);
    }

    private String formatProjectConfirmation(ProjectCreationState state) {
        return """
        <b>✅ ПОДТВЕРЖДЕНИЕ СОЗДАНИЯ ПРОЕКТА</b>

        <blockquote><b>🎯 Название:</b> %s

        <b>📝 Описание:</b>
        <i>%s</i>

        <b>💰 Бюджет:</b> <code>%.0f руб</code>
        <b>⏱️ Срок:</b> <code>%d дней</code>

        <b>🛠️ Требуемые навыки:</b>
        <u>%s</u></blockquote>

        <b>💡 Проверьте информацию перед созданием</b>
        <b>🚀 После создания проект станет доступен исполнителям</b>
        """.formatted(
                escapeHtml(state.getTitle()),
                escapeHtml(state.getDescription()),
                state.getBudget(),
                state.getEstimatedDays(),
                escapeHtml(state.getRequiredSkills())
        );
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