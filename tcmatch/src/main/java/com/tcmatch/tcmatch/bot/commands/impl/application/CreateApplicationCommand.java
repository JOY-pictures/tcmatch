package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.service.ApplicationCreationService;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class CreateApplicationCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationCreationService applicationCreationService;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "create".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long projectId = Long.parseLong(context.getParameter());
            ProjectDto project = projectService.getProjectDtoById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());

            // 🔥 УДАЛЯЕМ ВСЕ СООБЩЕНИЯ С ПРОЕКТАМИ И ПАГИНАЦИЕЙ (используем метод из BaseHandler)
            botExecutor.deletePreviousMessages(context.getChatId());

            // 🔥 СОХРАНЯЕМ MESSAGE_ID ПЕРЕД НАЧАЛОМ ПРОЦЕССА
            if (userSessionService.getMainMessageId(context.getChatId()) == null) {
                userSessionService.setMainMessageId(context.getChatId(), context.getMessageId());
            }


            // Проверяем, не откликался ли уже
            boolean hasApplied = applicationService.getUserApplications(context.getChatId())
                    .stream()
                    .anyMatch(app -> app.getProjectId().equals(projectId));

            if (hasApplied) {
                String text = "<b>❌ Вы уже откликались на этот проект</b>";
                botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId != null ? mainMessageId : context.getMessageId(), text, commonKeyboards.createBackButton());
                return;
            }

            // 🔥 ИСПОЛЬЗУЕМ ApplicationCreationService (который внутри использует UserSessionService)
            applicationCreationService.startApplicationCreation(context.getChatId(), projectId);
            showCurrentStep(context.getChatId(), project, mainMessageId);

        } catch (Exception e) {
            log.error("❌ Ошибка начала создания отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка начала создания отклика: " + e.getMessage(), 5);
        }
    }

    // 🔥 ОБНОВЛЯЕМ ПОКАЗ ШАГОВ С УЧЕТОМ РЕЖИМА РЕДАКТИРОВАНИЯ
    private void showCurrentStep(Long chatId, ProjectDto project, Integer messageId) {
        ApplicationCreationState state = applicationCreationService.getCurrentState(chatId);
        if (state == null) return;

        String text = "";
        InlineKeyboardMarkup keyboard = null;

        if (state.isEditing()) {
            // 🔥 РЕЖИМ РЕДАКТИРОВАНИЯ ОДНОГО ПОЛЯ
            text = getHtmlEditStepText(state, project);
            keyboard = applicationKeyboards.createApplicationEditKeyboard(state.getCurrentStep().name().toLowerCase(), state.getProjectId());
        } else if (state.getCurrentStep() == ApplicationCreationState.ApplicationCreationStep.CONFIRMATION) {
            // 🔥 ЭКРАН ПОДТВЕРЖДЕНИЯ - ВОЗМОЖНОСТЬ РЕДАКТИРОВАТЬ ВСЕ ПОЛЯ
            text = formatHtmlApplicationConfirmation(state, project);
            keyboard = applicationKeyboards.createApplicationConfirmationKeyboard(state.getProjectId());
        } else {
            // 🔥 ПРОЦЕСС ЗАПОЛНЕНИЯ - ТОЛЬКО ОТМЕНА
            text = getHtmlStepText(state, project);
            keyboard = applicationKeyboards.createApplicationProcessKeyboard(state.getCurrentStep().name().toLowerCase(), state.getProjectId());
        }

        botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);
    }

    private String getHtmlEditStepText(ApplicationCreationState state, ProjectDto project) {
        String currentValue = "";
        String instruction = "";

        switch (state.getCurrentStep()) {
            case DESCRIPTION:
                currentValue = state.getCoverLetter() != null ?
                        escapeHtml(state.getCoverLetter().length() > 100 ?
                                state.getCoverLetter().substring(0, 100) + "..." :
                                state.getCoverLetter()) :
                        "<i>не указано</i>";
                instruction = "<b>✍️ Введите новое описание:</b>";
                break;
            case BUDGET:
                currentValue = state.getProposedBudget() != null ?
                        "<code>" + state.getProposedBudget() + " руб</code>" :
                        "<i>не указан</i>";
                instruction = "<b>💸 Введите новый бюджет в рублях:</b>";
                break;
            case DEADLINE:
                currentValue = state.getProposedDays() != null ?
                        "<code>" + state.getProposedDays() + " дней</code>" :
                        "<i>не указан</i>";
                instruction = "<b>⏰ Введите новые сроки в днях:</b>";
                break;
            default:
                return "";
        }

        return """
        <b>✏️ РЕДАКТИРОВАНИЕ ОТКЛИКА</b>
        
        <b>💼 Проект:</b> %s
        
        <b>📊 Текущее значение:</b>
        %s
        
        %s
        
        <i>💡 После ввода вы вернетесь к подтверждению</i>
        """.formatted(
                escapeHtml(project.getTitle()),
                currentValue,
                instruction
        );
    }

    // 🔥 ТЕКСТ ДЛЯ ОБЫЧНОГО ПРОЦЕССА (оставляем как было)
    private String getHtmlStepText(ApplicationCreationState state, ProjectDto project) {
        switch (state.getCurrentStep()) {
            case DESCRIPTION:
                return """
                <b>📝 ШАГ 1: ОПИСАНИЕ ОТКЛИКА</b>
                
                <b>💼 Проект:</b> %s
                <b>💰 Бюджет проекта:</b> <code>%.0f руб</code>
                <b>⏱️ Срок проекта:</b> <code>%d дней</code>
                
                <b>✍️ Что нужно сделать:</b>
                • Напишите сопроводительное письмо
                • Расскажите о своем опыте
                • Объясните, почему подходите для проекта
                • Укажите ваши сильные стороны
                
                <i>💡 Совет: Персонализированные отклики получают в 3 раза больше ответов!</i>
                
                <b>👇 Отправьте ваше описание в следующем сообщении</b>
                """.formatted(
                        escapeHtml(project.getTitle()),
                        project.getBudget(),
                        project.getEstimatedDays()
                );

            case BUDGET:
                String currentDescription = state.getCoverLetter() != null ?
                        (state.getCoverLetter().length() > 100 ?
                                escapeHtml(state.getCoverLetter().substring(0, 100)) + "..." :
                                escapeHtml(state.getCoverLetter())) :
                        "<i>не указано</i>";

                return """
                <b>💰 ШАГ 2: ВАШ БЮДЖЕТ</b>
                
                <b>💼 Проект:</b> %s
                <b>📝 Ваше описание:</b> %s
                
                <b>💵 Бюджет проекта:</b> <code>%.0f руб</code>
                <b>💡 Ваше предложение:</b> %s
                
                <b>💸 Что нужно сделать:</b>
                • Напишите ваш бюджет в рублях
                • Можете предложить ту же сумму
                • Или указать вашу цену
                • Учитывайте сложность работы
                
                <b>👇 Отправьте число в следующем сообщении</b>
                """.formatted(
                        escapeHtml(project.getTitle()),
                        currentDescription,
                        project.getBudget(),
                        state.getProposedBudget() != null ?
                                "<code>" + state.getProposedBudget() + " руб</code>" :
                                "<i>не указан</i>"
                );

            case DEADLINE:
                return """
                <b>⏱️ ШАГ 3: СРОКИ ВЫПОЛНЕНИЯ</b>
                
                <b>💼 Проект:</b> %s
                <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>
                
                <b>📅 Срок проекта:</b> <code>%d дней</code>
                <b>🗓️ Ваше предложение:</b> %s
                
                <b>⏰ Что нужно сделать:</b>
                • Напишите срок выполнения в днях
                • Можете предложить те же сроки
                • Или указать реалистичное время
                • Учитывайте объем работы
                
                <b>👇 Отправьте число в следующем сообщении</b>
                """.formatted(
                        escapeHtml(project.getTitle()),
                        state.getProposedBudget() != null ? state.getProposedBudget() : project.getBudget(),
                        project.getEstimatedDays(),
                        state.getProposedDays() != null ?
                                "<code>" + state.getProposedDays() + " дней</code>" :
                                "<i>не указан</i>"
                );

            default:
                return "";
        }
    }

    private String formatHtmlApplicationConfirmation(ApplicationCreationState state, ProjectDto project) {
        return """
            <b>✅ ПОДТВЕРЖДЕНИЕ ОТКЛИКА</b>
        
        <blockquote><b>💼 Проект:</b> %s
        <b>👔 Заказчик:</b> @%s
        
        <b>📝 Ваше описание:</b>
        <i>%s</i>
        
        <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>
        <b>⏱️ Ваш срок:</b> <code>%d дней</code></blockquote>
        <b>💡 Проверьте информацию перед отправкой</b>
        <b>🛡️ После отправки изменить отклик будет нельзя</b>
        
        <b>⚠️ Внимание:</b> Использован 1 отклик из вашего лимита
        """.formatted(
                escapeHtml(project.getTitle()),
                project.getCustomerUserName() != null ?
                        escapeHtml(project.getCustomerUserName()) : "скрыт",
                escapeHtml(state.getCoverLetter()),
                state.getProposedBudget(),
                state.getProposedDays()
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
