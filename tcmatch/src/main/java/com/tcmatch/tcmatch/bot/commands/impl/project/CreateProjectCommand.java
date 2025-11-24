package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import com.tcmatch.tcmatch.service.ProjectCreationService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class CreateProjectCommand implements Command {

    private final BotExecutor botExecutor;
    private final ProjectCreationService projectCreationService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "create".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        try {
            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ
            botExecutor.deletePreviousMessages(chatId);

            // 🔥 ЗАПУСКАЕМ ПРОЦЕСС СОЗДАНИЯ
            projectCreationService.startProjectCreation(chatId);
            showCurrentProjectCreationStep(context);

        } catch (Exception e) {
            log.error("❌ Ошибка начала создания проекта: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка начала создания проекта", 5);
        }
    }

    private void showCurrentProjectCreationStep(CommandContext context) {
        Long chatId = context.getChatId();
        ProjectCreationState state = projectCreationService.getCurrentState(chatId);

        if (state == null) return;

        String text = "";
        InlineKeyboardMarkup keyboard = null;

        if (state.isEditing()) {
            // 🔥 РЕЖИМ РЕДАКТИРОВАНИЯ
            text = getProjectEditStepInfo(state);
            keyboard = projectKeyboards.createProjectEditKeyboard(state.getCurrentStep().name().toLowerCase());
        } else if (state.getCurrentStep() == ProjectCreationState.ProjectCreationStep.CONFIRMATION) {
            // 🔥 ЭКРАН ПОДТВЕРЖДЕНИЯ
            text = formatProjectConfirmation(state);
            keyboard = projectKeyboards.createProjectConfirmationKeyboard();
        } else {
            // 🔥 ПРОЦЕСС ЗАПОЛНЕНИЯ
            text = getProjectStepText(state);
            keyboard = projectKeyboards.createProjectCreationKeyboard();
        }

        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.editMessageWithHtml(chatId, mainMessageId, text, keyboard);
    }

    private String getProjectEditStepInfo(ProjectCreationState state) {
        String currentValue = "";
        String instruction = "";
        switch (state.getCurrentStep()) {
            case TITLE:
                currentValue = state.getTitle() != null ?
                        state.getTitle() : "<i>не указано</i>";
                instruction = "<b>✏️ Введите новое название проекта:</b>";
                break;
            case DESCRIPTION:
                currentValue = state.getDescription() != null ?
                        state.getDescription() :
                        "<i>не указано</i>";
                instruction = "<b>📝 Введите новое описание проекта:</b>";
                break;
            case BUDGET:
                currentValue = state.getBudget() != null ?
                        "<code>" + state.getBudget() + " руб</code>" :
                        "<i>не указан</i>";
                instruction = "<b>💰 Введите новый бюджет в рублях:</b>";
                break;
            case DEADLINE:
                currentValue = state.getEstimatedDays() != null ?
                        "<code>" + state.getEstimatedDays() + " дней</code>" :
                        "<i>не указан</i>";
                instruction = "<b>⏱️ Введите новые сроки в днях:</b>";
                break;
            case SKILLS:
                currentValue = state.getRequiredSkills() != null ?
                        state.getRequiredSkills() :
                        "<i>не указаны</i>";
                instruction = "<b>🛠️ Введите новые требуемые навыки:</b>";
                break;
            default:
                return "";
        }

        return """
        <b>✏️ РЕДАКТИРОВАНИЕ ПРОЕКТА</b>

        <b>📊 Текущее значение:</b>
        %s

        %s

        <i>💡 После ввода вы вернетесь к подтверждению</i>
        """.formatted(currentValue, instruction);
    }

    // 🔥 ТЕКСТ ДЛЯ ОБЫЧНОГО ПРОЦЕССА
    private String getProjectStepText(ProjectCreationState state) {
        switch (state.getCurrentStep()) {
            case TITLE:
                return """
                <b>📝 ШАГ 1: НАЗВАНИЕ ПРОЕКТА</b>

                <b>✏️ Что нужно сделать:</b>
                • Придумайте краткое и понятное название
                • Отразите суть проекта в названии
                • Максимум 100 символов

                <b>👇 Введите название проекта в следующем сообщении</b>
                """;

            case DESCRIPTION:
                String currentTitle = state.getTitle() != null ?
                        state.getTitle() : "<i>не указано</i>";

                return """
                <b>📋 ШАГ 2: ОПИСАНИЕ ПРОЕКТА</b>

                <b>🎯 Название проекта:</b> %s

                <b>📝 Что нужно сделать:</b>
                • Подробно опишите задачу
                • Укажите требования и ожидания
                • Опишите желаемый результат
                • Минимум 20 символов, максимум 3200

                <b>👇 Введите описание проекта в следующем сообщении</b>
                """.formatted(currentTitle);

            case BUDGET:
                String currentDescription = state.getDescription() != null ?
                        (state.getDescription().length() > 100 ?
                                state.getDescription().substring(0, 100) + "..." :
                                state.getDescription()) :
                        "<i>не указано</i>";

                return """
                <b>💰 ШАГ 3: БЮДЖЕТ ПРОЕКТА</b>

                <b>🎯 Название проекта:</b> %s
                <b>📝 Описание:</b> %s

                <b>💸 Что нужно сделать:</b>
                • Укажите бюджет в рублях
                • Минимальный бюджет: 1000 руб
                • Максимальный бюджет: 1 000 000 руб

                <b>👇 Введите бюджет в следующем сообщении</b>
                """.formatted(
                        state.getTitle(),
                        currentDescription
                );

            case DEADLINE:
                return """
                <b>⏱️ ШАГ 4: СРОК ВЫПОЛНЕНИЯ</b>

                <b>🎯 Название проекта:</b> %s
                <b>💰 Бюджет:</b> <code>%.0f руб</code>

                <b>📅 Что нужно сделать:</b>
                • Укажите срок выполнения в днях
                • Минимум: 1 день
                • Максимум: 365 дней

                <b>👇 Введите срок выполнения в следующем сообщении</b>
                """.formatted(
                        state.getTitle(),
                        state.getBudget()
                );

            case SKILLS:
                return """
                <b>🛠️ ШАГ 5: ТРЕБУЕМЫЕ НАВЫКИ</b>

                <b>🎯 Название проекта:</b> %s
                <b>💰 Бюджет:</b> <code>%.0f руб</code>
                <b>⏱️ Срок:</b> <code>%d дней</code>

                <b>🔧 Что нужно сделать:</b>
                • Перечислите требуемые навыки
                • Укажите технологии, инструменты
                • Опишите опыт, который нужен
                • Можно перечислить через запятую

                <b>👇 Введите требуемые навыки в следующем сообщении</b>
                """.formatted(
                        state.getTitle(),
                        state.getBudget(),
                        state.getEstimatedDays()
                );

            default:
                return "";
        }
    }

    // 🔥 ФОРМАТИРОВАНИЕ ПОДТВЕРЖДЕНИЯ
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
                state.getTitle(),
                state.getDescription(),
                state.getBudget(),
                state.getEstimatedDays(),
                state.getRequiredSkills()
        );
    }
}
