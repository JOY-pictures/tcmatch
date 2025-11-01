package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectData;
import com.tcmatch.tcmatch.model.enums.SubscriptionPlan;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class ApplicationHandler extends BaseHandler {

    private final SubscriptionService subscriptionService;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final ApplicationCreationService applicationCreationService;
    public ApplicationHandler(KeyboardFactory keyboardFactory, SubscriptionService subscriptionService,
                              ApplicationService applicationService, ProjectService projectService,
                              UserSessionService userSessionService, ApplicationCreationService applicationCreationService) {
        super(keyboardFactory, userSessionService);
        this.subscriptionService = subscriptionService;
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
            case "confirm_withdraw": // 🔥 НОВЫЙ CASE - ПОДТВЕРЖДЕНИЕ ОТЗЫВА
                confirmWithdrawApplication(data, parameter);
                break;
            case "details":
                showApplicationDetails(data, parameter);
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

            // 🔥 ИСПОЛЬЗУЕМ ApplicationCreationService (который внутри использует UserSessionService)
            applicationCreationService.startApplicationCreation(data.getChatId(), projectId);
            showCurrentStep(data, project);

        } catch (Exception e) {
            log.error("❌ Ошибка начала создания отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка начала создания отклика: " + e.getMessage(), 5);
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

            if (state.isEditing()) {
                state.finishEditing();
                applicationCreationService.updateCurrentState(chatId, state);
            } else {
                state.moveToNextStep();
                applicationCreationService.updateCurrentState(chatId, state);
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

            // 🔥 РЕАЛЬНАЯ ПРОВЕРКА ПОДПИСКИ И ЛИМИТОВ
            SubscriptionService.SubscriptionCheckResult subscriptionCheck =
                    subscriptionService.checkApplicationLimits(data.getChatId());

            if (!subscriptionCheck.canApply) {
                String warningText = createSubscriptionWarningText(subscriptionCheck);
                editMessage(data.getChatId(), data.getMessageId(), warningText,
                        keyboardFactory.createSubscriptionKeyboard());
                return;
            }

            // 🔥 ИСПОЛЬЗУЕМ ОТКЛИК (уменьшаем лимит)
            boolean applicationUsed = subscriptionService.useApplication(data.getChatId());
            if (!applicationUsed) {
                sendTemporaryErrorMessage(data.getChatId(), "❌ Не удалось использовать отклик", 5);
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

            // 🔥 ОБНОВЛЯЕМ СТАТИСТИКУ ДЛЯ СООБЩЕНИЯ УСПЕХА
            SubscriptionService.SubscriptionCheckResult updatedStats =
                    subscriptionService.checkApplicationLimits(data.getChatId());

            String successText = """
                    <b>✅ ОТКЛИК ОТПРАВЛЕН!</b>

    <blockquote><b>💼 Проект:</b> %s
    <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>  
    <b>⏱️ Ваш срок:</b> <code>%d дней</code>

    <b>📨 Статус:</b> отправлен заказчику
    <b>⏳ Ожидание:</b> ответа от заказчика </blockquote>

    <b>📊 Осталось откликов в этом месяце:</b> <code>%d/%d</code>

    <i>💡 Лимит обновится %s</i>
    """.formatted(
                    escapeHtml(application.getProject().getTitle()),
                    application.getProposedBudget(),
                    application.getProposedDays(),
                    updatedStats.remainingApplications,
                    updatedStats.currentPlan.getMonthlyApplicationsLimit(),
                    formatNextResetDate()
            );

            Integer mainMessageId = getMainMessageId(data.getChatId());
            editMessageWithHtml(data.getChatId(), mainMessageId, successText, keyboardFactory.createToMainMenuKeyboard());

            log.info("✅ Пользователь {} откликнулся на проект {}", data.getChatId(), state.getProjectId());

        } catch (Exception e) {
            log.error("❌ Ошибка подтверждения отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отправки отклика: " + e.getMessage(), 5);
        }
    }

    // 🔥 ТЕКСТ ПРЕДУПРЕЖДЕНИЯ О ЛИМИТАХ
    private String createSubscriptionWarningText(SubscriptionService.SubscriptionCheckResult check) {
        return """
        ⚠️ **ЛИМИТ ОТКЛИКОВ ИСЧЕРПАН**
        
        📊 Ваш текущий тариф: *%s*
        🚫 Использовано откликов: *%d/%d*
        
        💎 *Что делать:*
        • Приобрести подписку TCMatch Pro
        • Дождаться обновления лимита (1 числа)
        • Использовать отклики экономнее
        
        🛒 *Доступные тарифы:*
        • %s - %s
        • %s - %s  
        • %s - %s
        
        💡 *Подписка открывает:*
        • Больше откликов в месяц
        • Приоритет в поиске
        • Расширенную статистику
        """.formatted(
                check.currentPlan.getDisplayName(),
                check.currentPlan.getMonthlyApplicationsLimit() - check.remainingApplications,
                check.currentPlan.getMonthlyApplicationsLimit(),
                SubscriptionPlan.BASIC.getDisplayName(),
                SubscriptionPlan.BASIC.getPriceDisplay(),
                SubscriptionPlan.PRO.getDisplayName(),
                SubscriptionPlan.PRO.getPriceDisplay(),
                SubscriptionPlan.UNLIMITED.getDisplayName(),
                SubscriptionPlan.UNLIMITED.getPriceDisplay()
        );
    }

    // 🔥 ФОРМАТИРОВАНИЕ ДАТЫ ОБНОВЛЕНИЯ ЛИМИТОВ
    private String formatNextResetDate() {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
        return nextMonth.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private void cancelApplicationCreation(ProjectData data) {

        applicationCreationService.cancelCreation(data.getChatId());

        String text = """
        ❌ **СОЗДАНИЕ ОТКЛИКА ОТМЕНЕНО**
        
        💡 Вы можете вернуться к проекту и создать отклик позже
        """;

        Integer mainMessageId = getMainMessageId(data.getChatId());

        editMessage(data.getChatId(), mainMessageId, text, keyboardFactory.createToMainMenuKeyboard());

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
                
                📊 *Ваш отклик был удален из системы*
                """;

            InlineKeyboardMarkup keyboard = keyboardFactory.createToMainMenuKeyboard();



            Integer mainMessageId = getMainMessageId(data.getChatId());
            if (mainMessageId != null) {
                editMessage(data.getChatId(), mainMessageId, successText, keyboard);
            } else {
                Integer newMessageId = sendInlineMessageReturnId(data.getChatId(), successText, keyboard);
                saveMainMessageId(data.getChatId(), newMessageId);
            }

            log.info("✅ Пользователь {} отозвал отклик {}", data.getChatId(), applicationId);
        } catch (Exception e) {
            log.error("❌ Ошибка отзыва отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отзыва отклика: " + e.getMessage(), 5);
        }
    }

    private void confirmWithdrawApplication(ProjectData data, String applicationIdParam) {
        try {
            Long applicationId = Long.parseLong(applicationIdParam);
            Application application = applicationService.getApplicationById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Отклик не найден"));

            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ОТКЛИКА
            if (!application.getFreelancer().getChatId().equals(data.getChatId())) {
                sendTemporaryErrorMessage(data.getChatId(), "❌ У вас нет доступа к этому отклику", 5);
                return;
            }

            // 🔥 ПРОВЕРЯЕМ, ЧТО ОТКЛИК МОЖНО ОТОЗВАТЬ
            if (application.getStatus() != UserRole.ApplicationStatus.PENDING) {
                sendTemporaryErrorMessage(data.getChatId(),
                        "❌ Нельзя отозвать отклик со статусом: " + getApplicationStatusDisplay(application.getStatus()), 5);
                return;
            }

            String warningText = """
            <b>⚠️ **ПОДТВЕРЖДЕНИЕ ОТЗЫВА ОТКЛИКА**</b>
            
            <blockquote>📋 *Проект:* %s
            💰 *Ваш бюджет:* %.0f руб
            ⏱️ *Ваш срок:* %d дней
            📅 *Отправлен:* %s</blockquote>
            
            🔴<b> *Внимание! </b>После отзыва:*
            <i>• Отклик будет удален из системы
            • Заказчик больше не увидит ваш отклик
            • Вернуть отклик будет невозможно
            • Использованный отклик не вернется в лимит</i>
            
            ❓ <b>*Вы точно хотите отозвать этот отклик?*</b>
            """.formatted(
                    application.getProject().getTitle(),
                    application.getProposedBudget(),
                    application.getProposedDays(),
                    application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );
            InlineKeyboardMarkup keyboard = keyboardFactory.createWithdrawConfirmationKeyboard(applicationId);

            Integer mainMessageId = getMainMessageId(data.getChatId());
            if (mainMessageId != null) {
                editMessageWithHtml(data.getChatId(), mainMessageId, warningText, keyboard);
            } else {
                Integer newMessageId = sendInlineMessageReturnId(data.getChatId(), warningText, keyboard);
                saveMainMessageId(data.getChatId(), newMessageId);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка подтверждения отзыва отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка подтверждения отзыва", 5);
        }
    }

    // 🔥 МЕТОД ДЛЯ ПОКАЗА ДЕТАЛЕЙ ОТКЛИКА
    private void showApplicationDetails(ProjectData data, String applicationIdParam) {
        try {
            Long applicationId = Long.parseLong(applicationIdParam);
            Application application = applicationService.getApplicationById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Отклик не найден"));

            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ОТКЛИКА
            if (!application.getFreelancer().getChatId().equals(data.getChatId())) {
                sendTemporaryErrorMessage(data.getChatId(), "❌ У вас нет доступа к этому отклику", 5);
                return;
            }

            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ
            deletePreviousProjectMessages(data.getChatId());

            String applicationText = formatApplicationDetails(application);
            InlineKeyboardMarkup keyboard = keyboardFactory.createApplicationDetailsKeyboard(
                    application.getId(),
                    application.getStatus()
            );

            // 🔥 СОХРАНЯЕМ MESSAGE_ID ЕСЛИ ЕЩЁ НЕТ
            if (getMainMessageId(data.getChatId()) == null) {
                saveMainMessageId(data.getChatId(), data.getMessageId());
            }

            Integer mainMessageId = getMainMessageId(data.getChatId());

            if (mainMessageId != null) {
                editMessageWithHtml(data.getChatId(), mainMessageId, applicationText, keyboard);
            } else {
                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), applicationText, keyboard);
                saveMainMessageId(data.getChatId(), newMessageId);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка показа деталей отклика: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки информации об отклике", 5);
        }
    }

    // 🔥 ФОРМАТИРОВАНИЕ ДЕТАЛЕЙ ОТКЛИКА
    private String formatApplicationDetails(Application application) {
        Project project = application.getProject();

        return """
        <b>📋 **ДЕТАЛИ ВАШЕГО ОТКЛИКА**</b>
        
        <blockquote><b>💼 *Проект:* %s</b>
        <b>👔 *Заказчик:* @%s</b>
        <b>⭐ *Рейтинг заказчика:* %.1f/5.0</b>
        
        <b>💰 *Ваше предложение по бюджету:* %.0f руб</b>
        <b>💵 *Бюджет проекта:* %.0f руб</b>
        
        <b>⏱️ *Ваш срок выполнения:* %d дней</b>
        <b>📅 *Срок проекта:* %d дней</b>
        
        <b>📅 *Отклик отправлен:* %s</b>
        <b>📊 *Статус:* %s</b>
        <b>%s</b>
        <b>📝 *Ваше сопроводительное письмо:*</b>
        <i>%s</i>
        
        <b>🛠️ *Требуемые навыки:*</b>
        <u>%s</u></blockquote>
        """.formatted(
                project.getTitle(),
                project.getCustomer().getUsername() != null ? project.getCustomer().getUsername() : "скрыт",
                project.getCustomer().getProfessionalRating(),
                application.getProposedBudget(),
                project.getBudget(),
                application.getProposedDays(),
                project.getEstimatedDays(),
                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                getApplicationStatusDisplay(application.getStatus()),
                getApplicationStatusDetails(application),
                application.getCoverLetter(),
                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны"
        );
    }

    private String getApplicationStatusDisplay(UserRole.ApplicationStatus applicationStatus) {
        return switch (applicationStatus) {
            case PENDING -> "Ожидает рассмотрения";
            case ACCEPTED -> "Принят заказчиком";
            case REJECTED -> "Отклонен заказчиком";
            case WITHDRAWN -> "Отозван исполнителем";
        };
    }

    // 🔥 ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ О СТАТУСЕ
    private String getApplicationStatusDetails(Application application) {
        if (application.getReviewedAt() != null && application.getCustomerComment() != null) {
            return "💬 *Комментарий заказчика:* " + application.getCustomerComment() + "\n";
        }
        return "";
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
