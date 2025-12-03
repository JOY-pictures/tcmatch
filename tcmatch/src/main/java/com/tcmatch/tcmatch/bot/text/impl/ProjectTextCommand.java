package com.tcmatch.tcmatch.bot.text.impl;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.bot.text.TextCommand;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import com.tcmatch.tcmatch.service.ProjectCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectTextCommand implements TextCommand {

    private final BotExecutor botExecutor;
    private final ProjectCreationService projectCreationService;
    private final ProjectKeyboards projectKeyboards;

    @Override
    public boolean canHandle(Long chatId, String text) {
        return projectCreationService.isCreatingProject(chatId);
    }

    @Override
    public void execute(Message message) {
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();
        String text = message.getText();

        ProjectCreationState state = projectCreationService.getCurrentState(chatId);
        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);

        if (state == null) {
            botExecutor.deleteMessage(chatId, messageId);
            return;
        }

        // Сообщение, которое могло остаться после предыдущей ошибки
        Integer oldMessageIdToDelete = state.getMessageIdToDelete();

        try {
            // 1. ВАЛИДАЦИЯ и СОХРАНЕНИЕ ДАННЫХ
            projectCreationService.processInputAndValidate(state, text);

            // 2. УСПЕХ: Ввод принят
            if (oldMessageIdToDelete != null) {
                botExecutor.deleteMessage(chatId, oldMessageIdToDelete);
            }

            // 🔥 Удаление текущего успешного сообщения
            botExecutor.deleteMessage(chatId, messageId);

            // Очистка
            state.setMessageIdToDelete(null);

            // Переход: обновляем состояние и переходим к следующему шагу
            if (state.isEditing()) {
                state.finishEditing();
            } else {
                state.moveToNextStep();
            }

            projectCreationService.updateCurrentState(chatId, state);

            showCurrentStep(chatId, mainMessageId);

        } catch (NumberFormatException e) {
            // Ошибка валидации чисел (БЮДЖЕТ/СРОКИ)
            handleNumberFormatError(chatId, messageId, state, oldMessageIdToDelete, e);
        } catch (Exception e) {
            // Общая ошибка валидации
            handleGenericError(chatId, messageId, state, oldMessageIdToDelete, e);
        }
    }

    private void handleNumberFormatError(Long chatId, Integer messageId,
                                         ProjectCreationState state,
                                         Integer oldMessageIdToDelete,
                                         NumberFormatException e) {
        // Удаляем старое сообщение с ошибкой если было
        if (oldMessageIdToDelete != null) {
            botExecutor.deleteMessage(chatId, oldMessageIdToDelete);
            state.setMessageIdToDelete(null);
            projectCreationService.updateCurrentState(chatId, state);
        }

        String errorMsg = "❌ Пожалуйста, введите корректное число";
        botExecutor.deleteMessage(chatId, messageId);
        botExecutor.sendTemporaryErrorMessage(chatId, errorMsg, 5);
    }

    private void handleGenericError(Long chatId, Integer messageId,
                                    ProjectCreationState state,
                                    Integer oldMessageIdToDelete,
                                    Exception e) {
        // Удаляем старое сообщение с ошибкой если было
        if (oldMessageIdToDelete != null) {
            botExecutor.deleteMessage(chatId, oldMessageIdToDelete);
            state.setMessageIdToDelete(null);
            projectCreationService.updateCurrentState(chatId, state);
        }

        botExecutor.deleteMessage(chatId, messageId);
        botExecutor.sendTemporaryErrorMessage(chatId, "❌ Ошибка: " + e.getMessage(), 5);
    }

    private void showCurrentStep(Long chatId, Integer messageId) {
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

        botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);
    }

    private String getProjectEditStepInfo(ProjectCreationState state) {
        String currentValue = "";
        String instruction = "";
        switch (state.getCurrentStep()) {
            case TITLE:
                currentValue = state.getTitle() != null ?
                        escapeHtml(state.getTitle()) : "<i>не указано</i>";
                instruction = "<b>✏️ Введите новое название проекта:</b>";
                break;
            case DESCRIPTION:
                currentValue = state.getDescription() != null ?
                        (state.getDescription().length() > 100 ?
                                escapeHtml(state.getDescription().substring(0, 100)) + "..." :
                                escapeHtml(state.getDescription())) :
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
                        escapeHtml(state.getRequiredSkills()) :
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
                        escapeHtml(state.getTitle()) : "<i>не указано</i>";

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
                                escapeHtml(state.getDescription().substring(0, 100)) + "..." :
                                escapeHtml(state.getDescription())) :
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
                        escapeHtml(state.getTitle()),
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
                        escapeHtml(state.getTitle()),
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
                        escapeHtml(state.getTitle()),
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