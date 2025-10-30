package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectData;
import com.tcmatch.tcmatch.service.ApplicationCreationService;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.NavigationService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
public class ApplicationHandler extends BaseHandler {

    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final ApplicationCreationService applicationCreationService;

    public ApplicationHandler(KeyboardFactory keyboardFactory, NavigationService navigationService,
                              ApplicationService applicationService, ProjectService projectService,
                              ApplicationCreationService applicationCreationService) {
        super(keyboardFactory, navigationService);
        this.applicationService = applicationService;
        this.projectService = projectService;
        this.applicationCreationService = applicationCreationService;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        ProjectData data = new ProjectData(chatId, messageId, userName);

        switch (action) {
            case "create":
                startApplicationCreation(data, parameter);
                break;
            case "edit_field":
                editApplicationField(data, parameter);
                break;
            case "edit_cancel": // 🔥 НОВЫЙ CASE
                cancelEditing(data);
                break;
            case "confirm":
                confirmApplication(data);
                break;
            case "cancel":
                cancelApplicationCreation(data);
                break;
            case "withdraw":
                withdrawApplication(data, parameter);
                break;
            default:
                log.warn("❌ Unknown application action: {}", action);
        }
    }


    public void startApplicationCreation(ProjectData data, String projectIdParam) {
        try {
            Long projectId = Long.parseLong(projectIdParam);
            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            // 🔥 УДАЛЯЕМ ВСЕ СООБЩЕНИЯ С ПРОЕКТАМИ И ПАГИНАЦИЕЙ (используем метод из BaseHandler)
            deletePreviousProjectMessages(data.getChatId());

            // 🔥 СОХРАНЯЕМ MESSAGE_ID ПЕРЕД НАЧАЛОМ ПРОЦЕССА
            if (getMainMessageId(data.getChatId()) == null) {
                saveMainMessageId(data.getChatId(), data.getMessageId());
            }

            // Проверяем, не откликался ли уже
            boolean hasApplied = applicationService.getUserApplications(data.getChatId())
                    .stream()
                    .anyMatch(app -> app.getProject().getId().equals(projectId));

            if (hasApplied) {
                String text = "❌ Вы уже откликались на этот проект";
                Integer mainMessageId = getMainMessageId(data.getChatId());
                editMessage(data.getChatId(), mainMessageId != null ? mainMessageId : data.getMessageId(), text, keyboardFactory.createBackButton());
                return;
            }

            applicationCreationService.startApplicationCreation(data.getChatId(), projectId);
            showCurrentStep(data, project);

        } catch (Exception e) {
            log.error("❌ Ошибка начала создания отклика: {}", e.getMessage());
            sendErrorMessage(data.getChatId(), "Ошибка начала создания отклика: " + e.getMessage());
        }
    }

    // 🔥 ОБНОВЛЯЕМ ПОКАЗ ШАГОВ С УЧЕТОМ РЕЖИМА РЕДАКТИРОВАНИЯ
    private void showCurrentStep(ProjectData data, Project project) {
        ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
        if (state == null) return;

        String text = "";
        InlineKeyboardMarkup keyboard = null;

        if (state.isEditing()) {
            // 🔥 РЕЖИМ РЕДАКТИРОВАНИЯ ОДНОГО ПОЛЯ
            text = getHtmlEditStepText(state, project);
            keyboard = keyboardFactory.createApplicationEditKeyboard(state.getCurrentStep().name().toLowerCase(), state.getProjectId());
        } else if (state.getCurrentStep() == ApplicationCreationState.ApplicationCreationStep.CONFIRMATION) {
            // 🔥 ЭКРАН ПОДТВЕРЖДЕНИЯ - ВОЗМОЖНОСТЬ РЕДАКТИРОВАТЬ ВСЕ ПОЛЯ
            text = formatHtmlApplicationConfirmation(state, project);
            keyboard = keyboardFactory.createApplicationConfirmationKeyboard(state.getProjectId());
        } else {
            // 🔥 ПРОЦЕСС ЗАПОЛНЕНИЯ - ТОЛЬКО ОТМЕНА
            text = getHtmlStepText(state, project);
            keyboard = keyboardFactory.createApplicationProcessKeyboard(state.getCurrentStep().name().toLowerCase(), state.getProjectId());
        }

        Integer mainMessageId = getMainMessageId(data.getChatId());
        if (mainMessageId != null) {
            editMessageWithHtml(data.getChatId(), mainMessageId, text, keyboard); // 🔥 ИСПОЛЬЗУЕМ HTML-ВЕРСИЮ
        } else {
            Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), text, keyboard); // 🔥 ИСПОЛЬЗУЕМ HTML-ВЕРСИЮ
            if (newMessageId != null) {
                saveMainMessageId(data.getChatId(), newMessageId);
            }
        }
    }

    // 🔥 ТЕКСТ ДЛЯ РЕЖИМА РЕДАКТИРОВАНИЯ
    private String getHtmlEditStepText(ApplicationCreationState state, Project project) {
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
    private String getHtmlStepText(ApplicationCreationState state, Project project) {
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


    private String formatHtmlApplicationConfirmation(ApplicationCreationState state, Project project) {
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
                project.getCustomer().getUsername() != null ?
                        escapeHtml(project.getCustomer().getUsername()) : "скрыт",
                escapeHtml(state.getCoverLetter()),
                state.getProposedBudget(),
                state.getProposedDays()
        );
    }


    // 🔥 ОБНОВЛЯЕМ ОБРАБОТКУ ТЕКСТОВЫХ СООБЩЕНИЙ
    public void handleTextMessage(Long chatId, String text) {
        if (!applicationCreationService.isCreatingApplication(chatId)) {
            return;
        }

        ApplicationCreationState state = applicationCreationService.getCurrentState(chatId);
        if (state == null) return;

        try {
            switch (state.getCurrentStep()) {
                case DESCRIPTION:
                    state.setCoverLetter(text);
                    break;
                case BUDGET:
                    double budget = Double.parseDouble(text.replace(",", "."));
                    state.setProposedBudget(budget);
                    break;
                case DEADLINE:
                    int days = Integer.parseInt(text);
                    state.setProposedDays(days);
                    break;
                default:
                    return;
            }

            applicationCreationService.updateCurrentState(chatId, state);

            // 🔥 ЕСЛИ РЕЖИМ РЕДАКТИРОВАНИЯ - ВОЗВРАЩАЕМСЯ НА ПОДТВЕРЖДЕНИЕ
            if (state.isEditing()) {
                state.finishEditing();
                applicationCreationService.updateCurrentState(chatId, state);
            } else {
                // 🔥 ЕСЛИ ОБЫЧНЫЙ ПРОЦЕСС - ПЕРЕХОДИМ К СЛЕДУЮЩЕМУ ШАГУ
                state.moveToNextStep();
            }

            Project project = projectService.getProjectById(state.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            ProjectData data = new ProjectData(chatId, null, "");
            showCurrentStep(data, project);

        } catch (NumberFormatException e) {
            sendTemporaryErrorMessage(chatId, "❌ Пожалуйста, введите корректное число", 5);
        } catch (Exception e) {
            log.error("❌ Ошибка обработки текстового сообщения: {}", e.getMessage());
            sendTemporaryErrorMessage(chatId, "Ошибка обработки данных: " + e.getMessage(), 5);
        }
    }

    private void editApplicationField(ProjectData data, String field) {
        try {
            ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
            if (state == null) return;

            // 🔥 ПЕРЕХОДИМ В РЕЖИМ РЕДАКТИРОВАНИЯ КОНКРЕТНОГО ПОЛЯ
            state.moveToEditField(field);
            applicationCreationService.updateCurrentState(data.getChatId(), state);

            Project project = projectService.getProjectById(state.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            showCurrentStep(data, project);

        } catch (Exception e) {
            log.error("❌ Ошибка редактирования поля отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка редактирования отклика", 5);
        }
    }

    // 🔥 МЕТОД ОТМЕНЫ РЕДАКТИРОВАНИЯ
    private void cancelEditing(ProjectData data) {
        try {
            ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
            if (state == null) return;

            // 🔥 ВОЗВРАЩАЕМСЯ В РЕЖИМ ПОДТВЕРЖДЕНИЯ
            state.finishEditing();
            applicationCreationService.updateCurrentState(data.getChatId(), state);

            Project project = projectService.getProjectById(state.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            showCurrentStep(data, project);

        } catch (Exception e) {
            log.error("❌ Ошибка отмены редактирования: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отмены редактирования", 5);
        }
    }

    private void confirmApplication(ProjectData data) {
        try {
            ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
            if (state == null) return;

            if (!state.isCompleted()) {
                sendTemporaryErrorMessage(data.getChatId(), "❌ Заполните все поля отклика", 5);
                return;
            }

            // 🔥 ПРОВЕРКА ПОДПИСКИ И ЛИМИТОВ (здесь будет логика проверки)
            boolean hasSubscription = true; // временно
            int remainingApplications = 5; // временно

            if (!hasSubscription && remainingApplications <= 0) {
                String warningText = """
                        ⚠️ **ЛИМИТ ОТКЛИКОВ ИСЧЕРПАН**
                        
                        У вас закончились бесплатные отклики
                        
                        💎 *Что делать:*
                        • Приобрести подписку TCMatch Pro
                        • Дождаться обновления лимита
                        • Использовать отклики экономнее
                        
                        🛒 *Подписка открывает:*
                        • Неограниченные отклики
                        • Приоритет в поиске
                        • Расширенную статистику
                        """;
                editMessage(data.getChatId(), data.getMessageId(), warningText,
                        keyboardFactory.createSubscriptionKeyboard());
                return;
            }

            // СОЗДАЕМ ОТКЛИК
            Application application = applicationService.createApplication(
                    state.getProjectId(),
                    data.getChatId(),
                    state.getCoverLetter(),
                    state.getProposedBudget(),
                    state.getProposedDays()
            );

            applicationCreationService.completeCreation(data.getChatId());

            String successText = """
                    <b>✅ ОТКЛИК ОТПРАВЛЕН!</b>
        
        <blockquote><b>💼 Проект:</b> %s
        <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>  
        <b>⏱️ Ваш срок:</b> <code>%d дней</code>
        
        <b>📨 Статус:</b> отправлен заказчику
        <b>⏳ Ожидание:</b> ответа от заказчика </blockquote>
        
        <b>💡 Что дальше:</b>
        • Заказчик рассмотрит ваш отклик
        • Вы получите уведомление о решении
        • Можете отозвать отклик в любое время
        
        <b>📊 Осталось откликов:</b> <code>%d</code>
        """.formatted(
                    escapeHtml(application.getProject().getTitle()),
                    application.getProposedBudget(),
                    application.getProposedDays(),
                    remainingApplications
            );

            Integer mainMessageId = getMainMessageId(data.getChatId());
            editMessageWithHtml(data.getChatId(), mainMessageId, successText, keyboardFactory.createToMainMenuKeyboard());

            log.info("✅ Пользователь {} откликнулся на проект {}", data.getChatId(), state.getProjectId());

        } catch (Exception e) {
            log.error("❌ Ошибка подтверждения отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отправки отклика: " + e.getMessage(), 5);
        }
    }

    private void cancelApplicationCreation(ProjectData data) {
        applicationCreationService.cancelCreation(data.getChatId());

        String text = """
        ❌ **СОЗДАНИЕ ОТКЛИКА ОТМЕНЕНО**
        
        💡 Вы можете вернуться к проекту и создать отклик позже
        """;

        Integer mainMessageId = getMainMessageId(data.getChatId());

        // 🔥 ПОКАЗЫВАЕМ ГЛАВНЫЙ ЭКРАН ВМЕСТО ПРОСТО КНОПКИ "НАЗАД"
        editMessage(data.getChatId(), mainMessageId, text, keyboardFactory.createToMainMenuKeyboard());

        // 🔥 СБРАСЫВАЕМ НАВИГАЦИЮ НА ГЛАВНЫЙ ЭКРАН
        navigationService.resetToMain(data.getChatId());

        log.info("❌ Пользователь {} отменил создание отклика", data.getChatId());
    }

    public void withdrawApplication(ProjectData data, String applicationIdParam) {
        try {
            Long applicationId = Long.parseLong(applicationIdParam);

            applicationService.withdrawApplication(applicationId, data.getChatId());

            String successText = """
                ↩️ **ОТКЛИК ОТОЗВАН**
                
                📨 Заявка успешно отозвана
                👔 Заказчик уведомлен
                
                💡 Вы можете откликнуться на этот проект 
                снова, если передумаете
                """;

            editMessage(data.getChatId(), data.getMessageId(), successText, keyboardFactory.createBackButton());
            log.info("✅ Пользователь {} отозвал отклик {}", data.getChatId(), applicationId);

        } catch (Exception e) {
            log.error("❌ Ошибка отзыва отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отзыва отклика: " + e.getMessage(), 5);
        }
    }

    // 🔥 ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ЭКРАНИРОВАНИЯ HTML
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
