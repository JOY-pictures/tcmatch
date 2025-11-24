//package com.tcmatch.tcmatch.bot.handlers;
//
//import com.tcmatch.tcmatch.bot.BotExecutor;
//import com.tcmatch.tcmatch.bot.exceptions.DescriptionTooLongException;
//import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
//import com.tcmatch.tcmatch.model.Application;
//import com.tcmatch.tcmatch.model.dto.*;
//import com.tcmatch.tcmatch.model.enums.SubscriptionPlan;
//import com.tcmatch.tcmatch.model.enums.UserRole;
//import com.tcmatch.tcmatch.service.*;
//import com.tcmatch.tcmatch.util.PaginationContextKeys;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.BiFunction;
//
//@Component
//@Slf4j
//public class ApplicationHandler extends BaseHandler {
//
//    private final SubscriptionService subscriptionService;
//    private final ApplicationService applicationService;
//    private final ProjectService projectService;
//    private final UserService userService;
//    private final PaginationManager paginationManager;
//    private final ApplicationCreationService applicationCreationService;
//
////    private static final String FREELANCER_APPLICATIONS_CONTEXT_KEY = "my_applications";
////    private static final String PROJECT_APPLICATIONS_CONTEXT_KEY = "project_applications";
//    private static final int APPLICATIONS_PER_PAGE = 3; // Пример
//
//    public ApplicationHandler(KeyboardFactory keyboardFactory, SubscriptionService subscriptionService,
//                              ApplicationService applicationService, ProjectService projectService,
//                              UserSessionService userSessionService, PaginationManager paginationManager,
//                              ApplicationCreationService applicationCreationService,
//                              BotExecutor botExecutor, UserService userService) {
//        super(botExecutor, keyboardFactory, userSessionService);
//        this.subscriptionService = subscriptionService;
//        this.applicationService = applicationService;
//        this.projectService = projectService;
//        this.paginationManager = paginationManager;
//        this.applicationCreationService = applicationCreationService;
//        this.userService = userService;
//    }
//
//    @Override
//    public boolean canHandle(String actionType, String action) {
//        return "application".equals(actionType);
//    }
//
//    @Override
//    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
//        ProjectData data = new ProjectData(chatId, messageId, userName);
//
//        switch (action) {
////            case "menu":
////                handleApplicationMenu(data);
////                break;
////            case "create":
////                startApplicationCreation(data, parameter);
////                break;
////            case "edit_field":
////                editApplicationField(data, parameter);
////                break;
////            case "edit_cancel": // 🔥 НОВЫЙ CASE
////                cancelEditing(data);
////                break;
////            case "confirm":
////                confirmApplication(data);
////                break;
////            case "cancel":
////                cancelApplicationCreation(data);
////                break;
////            case "withdraw":
////                withdrawApplication(data, parameter);
////                break;
////            case "confirm_withdraw": // 🔥 НОВЫЙ CASE - ПОДТВЕРЖДЕНИЕ ОТЗЫВА
////                confirmWithdrawApplication(data, parameter);
////                break;
////            case "details":
////                showApplicationDetails(data, parameter);
////            case "pagination":
////                handleApplicationPagination(data, parameter);
//            default:
//                log.warn("❌ Unknown application action: {}", action);
//        }
//    }
//
////    public void handleApplicationMenu(ProjectData data) {
////        Long chatId = data.getChatId();
////        Integer messageId = data.getMessageId();
////
////        UserRole userRole = userService.getUserRole(chatId);
////
////        if (userRole == UserRole.FREELANCER) {
////            // Показать список откликов, которые фрилансер отправил (Мои отклики)
////            handleShowMyApplications(chatId, messageId);
////        } else if (userRole == UserRole.CUSTOMER) {
////            // Показать список откликов, которые заказчик получил (Отклики на мои проекты)
////            handleShowProjectListApplications(chatId, messageId);
////        } else {
////            // Если роль не определена или не соответствует
////            log.warn("❌ User {} tried to access application menu with unsupported role: {}", chatId, userRole);
////            sendTemporaryErrorMessage(chatId, "Доступ к разделу 'Отклики' для вашей роли ограничен.", 5);
////        }
////    }
//
////    public void handleShowMyApplications(Long chatId, Integer messageId) {
////        try {
////            final String HISTORY_POINT = PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY; // "my_applications"
////            // 🔥 Якорь: Откат истории до этой точки
//////            userSessionService.rewindToHistoryPoint(chatId, HISTORY_POINT);
////
////            // 1. Получаем ID всех откликов фрилансера
////            List<Long> applicationIds = applicationService.getApplicationsByFreelancerChatId(chatId)
////                    .stream().map(Application::getId).toList();
////
////            if (applicationIds.isEmpty()) {
////                showNoApplicationsMessage(chatId, messageId, UserRole.FREELANCER);
////                return;
////            }
////
////            // 2. Запускаем пагинацию
////            paginationManager.renderIdBasedPage(
////                    chatId,
////                    HISTORY_POINT,
////                    applicationIds,
////                    "APPLICATION",
////                    "init",
////                    APPLICATIONS_PER_PAGE,
////                    this::renderFreelancerApplicationsPage // 🔥 Передаем рендерер фрилансера
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа откликов фрилансера: {}", e.getMessage());
////            sendTemporaryErrorMessage(chatId, "Ошибка загрузки ваших откликов", 5);
////        }
////    }
//
//    // 🔥 2. ВХОД В СПИСОК "ОТКЛИКИ НА МОИ ПРОЕКТЫ" (ЗАКАЗЧИК)
////    public void handleShowProjectListApplications(Long chatId, Integer messageId) {
////        try {
////            final String HISTORY_POINT = PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY; // "project_applications"
////            // 🔥 Якорь: Откат истории до этой точки
////            userSessionService.rewindToHistoryPoint(chatId, HISTORY_POINT);
////
////            // 1. Получаем ID всех проектов заказчика
////            List<Long> projectIds = projectService.getProjectIdsByCustomerChatId(chatId);
////
////            if (projectIds.isEmpty()) {
////                showNoApplicationsMessage(chatId, messageId, UserRole.CUSTOMER); // Нет проектов
////                return;
////            }
////
////            // 2. Получаем ID всех откликов на эти проекты
////            List<Long> applicationIds = applicationService.getApplicationsByProjectIds(projectIds)
////                    .stream().map(Application::getId).toList();
////
////            if (applicationIds.isEmpty()) {
////                showNoApplicationsMessage(chatId, messageId, UserRole.CUSTOMER); // Есть проекты, но нет откликов
////                return;
////            }
////
////            // 3. Запускаем пагинацию
////            paginationManager.renderIdBasedPage(
////                    chatId,
////                    HISTORY_POINT,
////                    applicationIds,
////                    "APPLICATION",
////                    "init",
////                    APPLICATIONS_PER_PAGE,
////                    this::renderProjectApplicationsPage // 🔥 Передаем рендерер заказчика
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа откликов на проекты заказчика: {}", e.getMessage());
////            sendTemporaryErrorMessage(chatId, "Ошибка загрузки откликов на ваши проекты", 5);
////        }
////    }
//
////    // 🔥 НОВЫЙ МЕТОД ДЛЯ ПОКАЗА ОТКЛИКОВ ФРИЛАНСЕРА С ПАГИНАЦИЕЙ ID
////    public void showFreelancerApplications(ProjectData data) {
////        try {
////            Long chatId = data.getChatId();
////
////            // 🔥 ПОЛУЧАЕМ ID ОТКЛИКОВ ВМЕСТО ПОЛНЫХ СУЩНОСТЕЙ
////            List<Long> applicationIds = applicationService.getUserApplicationIds(chatId);
////
////            if (applicationIds.isEmpty()) {
////                String text = """
////                📭 <b>ОТКЛИКОВ НЕТ</b>
////
////                💡 <i>Вы ещё не откликались на проекты</i>
////                """;
////                editMessageWithHtml(chatId, data.getMessageId(), text, keyboardFactory.createBackButton());
////                return;
////            }
////
////            // 🔥 ИСПОЛЬЗУЕМ PAGINATION MANAGER С ID
////            paginationManager.renderIdBasedPage(
////                    chatId,
////                    PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY,
////                    applicationIds,
////                    "APPLICATION",
////                    "init",
////                    APPLICATIONS_PER_PAGE,
////                    this::renderFreelancerApplicationsPage
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа откликов фрилансера: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки ваших откликов", 5);
////        }
////    }
//
//
//
////    public void startApplicationCreation(ProjectData data, String projectIdParam) {
////        try {
////            Long projectId = Long.parseLong(projectIdParam);
////            ProjectDto project = projectService.getProjectDtoById(projectId)
////                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
////
////            // 🔥 УДАЛЯЕМ ВСЕ СООБЩЕНИЯ С ПРОЕКТАМИ И ПАГИНАЦИЕЙ (используем метод из BaseHandler)
////            deletePreviousMessages(data.getChatId());
////
////            // 🔥 СОХРАНЯЕМ MESSAGE_ID ПЕРЕД НАЧАЛОМ ПРОЦЕССА
////            if (getMainMessageId(data.getChatId()) == null) {
////                saveMainMessageId(data.getChatId(), data.getMessageId());
////            }
////
////            // Проверяем, не откликался ли уже
////            boolean hasApplied = applicationService.getUserApplications(data.getChatId())
////                    .stream()
////                    .anyMatch(app -> app.getProjectId().equals(projectId));
////
////            if (hasApplied) {
////                String text = "<b>❌ Вы уже откликались на этот проект</b>";
////                Integer mainMessageId = getMainMessageId(data.getChatId());
////                editMessageWithHtml(data.getChatId(), mainMessageId != null ? mainMessageId : data.getMessageId(), text, keyboardFactory.createBackButton());
////                return;
////            }
////
////            // 🔥 ИСПОЛЬЗУЕМ ApplicationCreationService (который внутри использует UserSessionService)
////            applicationCreationService.startApplicationCreation(data.getChatId(), projectId);
////            showCurrentStep(data, project);
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка начала создания отклика: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка начала создания отклика: " + e.getMessage(), 5);
////        }
////    }
//
////    // 🔥 ОБНОВЛЯЕМ ПОКАЗ ШАГОВ С УЧЕТОМ РЕЖИМА РЕДАКТИРОВАНИЯ
////    private void showCurrentStep(ProjectData data, ProjectDto project) {
////        ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
////        if (state == null) return;
////
////        String text = "";
////        InlineKeyboardMarkup keyboard = null;
////
////        if (state.isEditing()) {
////            // 🔥 РЕЖИМ РЕДАКТИРОВАНИЯ ОДНОГО ПОЛЯ
////            text = getHtmlEditStepText(state, project);
////            keyboard = keyboardFactory.createApplicationEditKeyboard(state.getCurrentStep().name().toLowerCase(), state.getProjectId());
////        } else if (state.getCurrentStep() == ApplicationCreationState.ApplicationCreationStep.CONFIRMATION) {
////            // 🔥 ЭКРАН ПОДТВЕРЖДЕНИЯ - ВОЗМОЖНОСТЬ РЕДАКТИРОВАТЬ ВСЕ ПОЛЯ
////            text = formatHtmlApplicationConfirmation(state, project);
////            keyboard = keyboardFactory.createApplicationConfirmationKeyboard(state.getProjectId());
////        } else {
////            // 🔥 ПРОЦЕСС ЗАПОЛНЕНИЯ - ТОЛЬКО ОТМЕНА
////            text = getHtmlStepText(state, project);
////            keyboard = keyboardFactory.createApplicationProcessKeyboard(state.getCurrentStep().name().toLowerCase(), state.getProjectId());
////        }
////
////        Integer mainMessageId = getMainMessageId(data.getChatId());
////        if (mainMessageId != null) {
////            editMessageWithHtml(data.getChatId(), mainMessageId, text, keyboard); // 🔥 ИСПОЛЬЗУЕМ HTML-ВЕРСИЮ
////        } else {
////            Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), text, keyboard); // 🔥 ИСПОЛЬЗУЕМ HTML-ВЕРСИЮ
////            if (newMessageId != null) {
////                saveMainMessageId(data.getChatId(), newMessageId);
////            }
////        }
////    }
//
//    // 🔥 ТЕКСТ ДЛЯ РЕЖИМА РЕДАКТИРОВАНИЯ
////    private String getHtmlEditStepText(ApplicationCreationState state, ProjectDto project) {
////        String currentValue = "";
////        String instruction = "";
////
////        switch (state.getCurrentStep()) {
////            case DESCRIPTION:
////                currentValue = state.getCoverLetter() != null ?
////                        escapeHtml(state.getCoverLetter().length() > 100 ?
////                                state.getCoverLetter().substring(0, 100) + "..." :
////                                state.getCoverLetter()) :
////                        "<i>не указано</i>";
////                instruction = "<b>✍️ Введите новое описание:</b>";
////                break;
////            case BUDGET:
////                currentValue = state.getProposedBudget() != null ?
////                        "<code>" + state.getProposedBudget() + " руб</code>" :
////                        "<i>не указан</i>";
////                instruction = "<b>💸 Введите новый бюджет в рублях:</b>";
////                break;
////            case DEADLINE:
////                currentValue = state.getProposedDays() != null ?
////                        "<code>" + state.getProposedDays() + " дней</code>" :
////                        "<i>не указан</i>";
////                instruction = "<b>⏰ Введите новые сроки в днях:</b>";
////                break;
////            default:
////                return "";
////        }
////
////        return """
////        <b>✏️ РЕДАКТИРОВАНИЕ ОТКЛИКА</b>
////
////        <b>💼 Проект:</b> %s
////
////        <b>📊 Текущее значение:</b>
////        %s
////
////        %s
////
////        <i>💡 После ввода вы вернетесь к подтверждению</i>
////        """.formatted(
////                escapeHtml(project.getTitle()),
////                currentValue,
////                instruction
////        );
////    }
//
////    // 🔥 ТЕКСТ ДЛЯ ОБЫЧНОГО ПРОЦЕССА (оставляем как было)
////    private String getHtmlStepText(ApplicationCreationState state, ProjectDto project) {
////        switch (state.getCurrentStep()) {
////            case DESCRIPTION:
////                return """
////                <b>📝 ШАГ 1: ОПИСАНИЕ ОТКЛИКА</b>
////
////                <b>💼 Проект:</b> %s
////                <b>💰 Бюджет проекта:</b> <code>%.0f руб</code>
////                <b>⏱️ Срок проекта:</b> <code>%d дней</code>
////
////                <b>✍️ Что нужно сделать:</b>
////                • Напишите сопроводительное письмо
////                • Расскажите о своем опыте
////                • Объясните, почему подходите для проекта
////                • Укажите ваши сильные стороны
////
////                <i>💡 Совет: Персонализированные отклики получают в 3 раза больше ответов!</i>
////
////                <b>👇 Отправьте ваше описание в следующем сообщении</b>
////                """.formatted(
////                        escapeHtml(project.getTitle()),
////                        project.getBudget(),
////                        project.getEstimatedDays()
////                );
////
////            case BUDGET:
////                String currentDescription = state.getCoverLetter() != null ?
////                        (state.getCoverLetter().length() > 100 ?
////                                escapeHtml(state.getCoverLetter().substring(0, 100)) + "..." :
////                                escapeHtml(state.getCoverLetter())) :
////                        "<i>не указано</i>";
////
////                return """
////                <b>💰 ШАГ 2: ВАШ БЮДЖЕТ</b>
////
////                <b>💼 Проект:</b> %s
////                <b>📝 Ваше описание:</b> %s
////
////                <b>💵 Бюджет проекта:</b> <code>%.0f руб</code>
////                <b>💡 Ваше предложение:</b> %s
////
////                <b>💸 Что нужно сделать:</b>
////                • Напишите ваш бюджет в рублях
////                • Можете предложить ту же сумму
////                • Или указать вашу цену
////                • Учитывайте сложность работы
////
////                <b>👇 Отправьте число в следующем сообщении</b>
////                """.formatted(
////                        escapeHtml(project.getTitle()),
////                        currentDescription,
////                        project.getBudget(),
////                        state.getProposedBudget() != null ?
////                                "<code>" + state.getProposedBudget() + " руб</code>" :
////                                "<i>не указан</i>"
////                );
////
////            case DEADLINE:
////                return """
////                <b>⏱️ ШАГ 3: СРОКИ ВЫПОЛНЕНИЯ</b>
////
////                <b>💼 Проект:</b> %s
////                <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>
////
////                <b>📅 Срок проекта:</b> <code>%d дней</code>
////                <b>🗓️ Ваше предложение:</b> %s
////
////                <b>⏰ Что нужно сделать:</b>
////                • Напишите срок выполнения в днях
////                • Можете предложить те же сроки
////                • Или указать реалистичное время
////                • Учитывайте объем работы
////
////                <b>👇 Отправьте число в следующем сообщении</b>
////                """.formatted(
////                        escapeHtml(project.getTitle()),
////                        state.getProposedBudget() != null ? state.getProposedBudget() : project.getBudget(),
////                        project.getEstimatedDays(),
////                        state.getProposedDays() != null ?
////                                "<code>" + state.getProposedDays() + " дней</code>" :
////                                "<i>не указан</i>"
////                );
////
////            default:
////                return "";
////        }
////    }
//
//
////    private String formatHtmlApplicationConfirmation(ApplicationCreationState state, ProjectDto project) {
////        return """
////            <b>✅ ПОДТВЕРЖДЕНИЕ ОТКЛИКА</b>
////
////        <blockquote><b>💼 Проект:</b> %s
////        <b>👔 Заказчик:</b> @%s
////
////        <b>📝 Ваше описание:</b>
////        <i>%s</i>
////
////        <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>
////        <b>⏱️ Ваш срок:</b> <code>%d дней</code></blockquote>
////        <b>💡 Проверьте информацию перед отправкой</b>
////        <b>🛡️ После отправки изменить отклик будет нельзя</b>
////
////        <b>⚠️ Внимание:</b> Использован 1 отклик из вашего лимита
////        """.formatted(
////                escapeHtml(project.getTitle()),
////                project.getCustomerUserName() != null ?
////                        escapeHtml(project.getCustomerUserName()) : "скрыт",
////                escapeHtml(state.getCoverLetter()),
////                state.getProposedBudget(),
////                state.getProposedDays()
////        );
////    }
//
//
////    public void handleTextMessage(Long chatId, String text, Integer messageId) {
////        if (!applicationCreationService.isCreatingApplication(chatId)) {
////            deleteMessage(chatId, messageId);
////        }
////
////        ApplicationCreationState state = applicationCreationService.getCurrentState(chatId);
////
////        if (state == null) {
////            deleteMessage(chatId, messageId);
////            return;
////        }
////
////        // M_old: Сообщение, которое могло остаться после предыдущей ошибки (для скользящего удаления)
////        Integer oldMessageIdToDelete = state.getMessageIdToDelete();
////
////        try {
////            // 1. ВАЛИДАЦИЯ и СОХРАНЕНИЕ ДАННЫХ
////            applicationCreationService.processInputAndValidate(state, text);
////
////            // 2. УСПЕХ: Ввод принят.
////
////            if (oldMessageIdToDelete != null) {
////                deleteMessage(chatId, oldMessageIdToDelete);
////            }
////
////            // 🔥 Удаление M_new (Текущее успешное сообщение)
////            deleteMessage(chatId, messageId);
////
////            // Очистка
////            state.setMessageIdToDelete(null); // Сброс ID
////
////            // Переход: обновляем состояние и переходим к следующему шагу
////            if (state.isEditing()) {
////                state.finishEditing();
////            } else {
////                state.moveToNextStep();
////            }
////
////            applicationCreationService.updateCurrentState(chatId, state);
////
////            ProjectDto project = projectService.getProjectDtoById(state.getProjectId())
////                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
////
////            ProjectData data = new ProjectData(chatId, null, "");
////            showCurrentStep(data, project);
////
////        } catch (DescriptionTooLongException e) {
////            // 1. ОШИБКА "СЛИШКОМ ДЛИННОЕ": Удаляем M_old (ID уже в списке idsToDelete)
////
////            // 🔥 Удаляем M_old (Предыдущее сообщение с ошибкой, если оно было)
////            if (oldMessageIdToDelete != null) {
////                deleteMessage(chatId, oldMessageIdToDelete);
////            }
////
////            // 🔥 НЕ удаляем M_new (Текущее сообщение), чтобы пользователь мог его видеть
////
////            // 2. Сохраняем ID ТЕКУЩЕГО сообщения (M_new), которое теперь остается
////            state.setMessageIdToDelete(messageId);
////            applicationCreationService.updateCurrentState(chatId, state);
////
////            // 3. Отображение ошибки
////            sendTemporaryErrorMessage(chatId, "⚠️ Ошибка: " + e.getMessage(), 10);
////
////        } catch (NumberFormatException e) {
////            // Ошибка валидации (БЮДЖЕТ/СРОКИ):
////
////            // 🔥 Удаляем M_old (Предыдущее сообщение с ошибкой, если оно было)
////            if (oldMessageIdToDelete != null) {
////                deleteMessage(chatId, oldMessageIdToDelete);
////                state.setMessageIdToDelete(null); // Сброс ID
////                applicationCreationService.updateCurrentState(chatId, state);
////            }
////
////            // Отправляем ошибку и сохраняем текущий шаг (он не меняется)
////            String errorMsg = e instanceof NumberFormatException ?
////                    "❌ Пожалуйста, введите корректное число" :
////                    "❌ Ошибка ввода: " + e.getMessage();
////            deleteMessage(chatId, messageId);
////            sendTemporaryErrorMessage(chatId, errorMsg, 5);
////
////        } catch (Exception e) {
////            log.error("❌ Критическая ошибка обработки текста: {}", e.getMessage());
////            // В случае критической ошибки удаляем сообщение пользователя
////            deleteMessage(chatId, messageId);
////            sendTemporaryErrorMessage(chatId, "Ошибка обработки данных: " + e.getMessage(), 5);
////        }
////    }
////
////    private void handleDescriptionInput(ApplicationCreationState state, String text) {
////        // 🔥 Новое требование: максимальная длина 3200 символов
////        if (text.length() < 10) {
////            throw new IllegalArgumentException("Описание должно содержать минимум 10 символов.");
////        }
////
////        // 🔥 ВАЛИДАЦИЯ ПРЕВЫШЕНИЯ ЛИМИТА
////        if (text.length() > 3200) {
////            // Мы НЕ сохраняем данные в state и выбрасываем специальное исключение.
////            // Это ИСКЛЮЧЕНИЕ будет поймано в ApplicationHandler, который не удалит сообщение пользователя.
////            throw new DescriptionTooLongException("Слишком длинное описание. Максимум 3200 символов. Пожалуйста, сократите и отправьте повторно.");
////        }
////        // Если валидация пройдена:
////        state.setCoverLetter(text);
////    }
//
////    private void editApplicationField(ProjectData data, String field) {
////        try {
////            ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
////            if (state == null) return;
////
////            // 🔥 ПЕРЕХОДИМ В РЕЖИМ РЕДАКТИРОВАНИЯ КОНКРЕТНОГО ПОЛЯ
////            state.moveToEditField(field);
////            applicationCreationService.updateCurrentState(data.getChatId(), state);
////
////            ProjectDto project = projectService.getProjectDtoById(state.getProjectId())
////                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
////
////            showCurrentStep(data, project);
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка редактирования поля отклика: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка редактирования отклика", 5);
////        }
////    }
//
////    // 🔥 МЕТОД ОТМЕНЫ РЕДАКТИРОВАНИЯ
////    private void cancelEditing(ProjectData data) {
////        try {
////            ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
////            if (state == null) return;
////
////            // 🔥 ВОЗВРАЩАЕМСЯ В РЕЖИМ ПОДТВЕРЖДЕНИЯ
////            state.finishEditing();
////            applicationCreationService.updateCurrentState(data.getChatId(), state);
////
////            ProjectDto project = projectService.getProjectDtoById(state.getProjectId())
////                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
////
////            showCurrentStep(data, project);
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка отмены редактирования: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отмены редактирования", 5);
////        }
////    }
//
////    private void confirmApplication(ProjectData data) {
////        try {
////            ApplicationCreationState state = applicationCreationService.getCurrentState(data.getChatId());
////            if (state == null) return;
////
////            if (!state.isCompleted()) {
////                sendTemporaryErrorMessage(data.getChatId(), "❌ Заполните все поля отклика", 5);
////                return;
////            }
////
////            // 🔥 РЕАЛЬНАЯ ПРОВЕРКА ПОДПИСКИ И ЛИМИТОВ
////            SubscriptionService.SubscriptionCheckResult subscriptionCheck =
////                    subscriptionService.checkApplicationLimits(data.getChatId());
////
////            if (!subscriptionCheck.canApply) {
////                String warningText = createSubscriptionWarningText(subscriptionCheck);
////                editMessageWithHtml(data.getChatId(), data.getMessageId(), warningText,
////                        keyboardFactory.createSubscriptionKeyboard());
////                return;
////            }
////
////            // 🔥 ИСПОЛЬЗУЕМ ОТКЛИК (уменьшаем лимит)
////            boolean applicationUsed = subscriptionService.useApplication(data.getChatId());
////            if (!applicationUsed) {
////                sendTemporaryErrorMessage(data.getChatId(), "❌ Не удалось использовать отклик", 5);
////                return;
////            }
////
////            // СОЗДАЕМ ОТКЛИК
////            Application application = applicationService.createApplication(
////                    state.getProjectId(),
////                    data.getChatId(),
////                    state.getCoverLetter(),
////                    state.getProposedBudget(),
////                    state.getProposedDays()
////            );
////
////            applicationCreationService.completeCreation(data.getChatId());
////
////            // 🔥 ОБНОВЛЯЕМ СТАТИСТИКУ ДЛЯ СООБЩЕНИЯ УСПЕХА
////            SubscriptionService.SubscriptionCheckResult updatedStats =
////                    subscriptionService.checkApplicationLimits(data.getChatId());
////
////            // 🔥 ПОЛУЧАЕМ ДАННЫЕ ПРОЕКТА ЧЕРЕЗ СЕРВИС
////            String projectTitle = projectService.getProjectTitleById(state.getProjectId());
////
////            String successText = """
////                    <b>✅ ОТКЛИК ОТПРАВЛЕН!</b>
////
////                    <blockquote><b>💼 Проект:</b> %s
////                    <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>
////                    <b>⏱️ Ваш срок:</b> <code>%d дней</code>
////
////                    <b>📨 Статус:</b> отправлен заказчику
////                    <b>⏳ Ожидание:</b> ответа от заказчика </blockquote>
////
////                    <b>📊 Осталось откликов в этом месяце:</b> <code>%d/%d</code>
////
////                    <i>💡 Лимит обновится %s</i>
////                    """.formatted(
////                    escapeHtml(projectTitle),
////                    application.getProposedBudget(),
////                    application.getProposedDays(),
////                    updatedStats.remainingApplications,
////                    updatedStats.currentPlan.getMonthlyApplicationsLimit(),
////                    formatNextResetDate()
////            );
////
////            Integer mainMessageId = getMainMessageId(data.getChatId());
////            editMessageWithHtml(data.getChatId(), mainMessageId, successText, keyboardFactory.createToMainMenuKeyboard());
////
////            log.info("✅ Пользователь {} откликнулся на проект {}", data.getChatId(), state.getProjectId());
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка подтверждения отклика: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отправки отклика: " + e.getMessage(), 5);
////        }
////    }
//
////    // 🔥 ТЕКСТ ПРЕДУПРЕЖДЕНИЯ О ЛИМИТАХ
////    private String createSubscriptionWarningText(SubscriptionService.SubscriptionCheckResult check) {
////        return """
////        ⚠️<b> **ЛИМИТ ОТКЛИКОВ ИСЧЕРПАН**</b>
////
////        📊 <b>Ваш текущий тариф: *%s*</b>
////        🚫 Использовано откликов: *%d/%d*
////
////        <b>💎 *Что делать:*</b>
////        • Приобрести подписку <b>TCMatch Pro</b>
////        • <i>Дождаться обновления лимита (1 числа)
////        • Использовать отклики экономнее</i>
////
////        🛒 <b>*Доступные тарифы:*</b>
////        •<i> %s - %s
////        • %s - %s
////        • %s - %s</i>
////
////        <b>💡 *Подписка открывает:*
////        • Больше откликов в месяц
////        • Приоритет в поиске
////        • Расширенную статистику</b>
////        """.formatted(
////                check.currentPlan.getDisplayName(),
////                check.currentPlan.getMonthlyApplicationsLimit() - check.remainingApplications,
////                check.currentPlan.getMonthlyApplicationsLimit(),
////                SubscriptionPlan.BASIC.getDisplayName(),
////                SubscriptionPlan.BASIC.getPriceDisplay(),
////                SubscriptionPlan.PRO.getDisplayName(),
////                SubscriptionPlan.PRO.getPriceDisplay(),
////                SubscriptionPlan.UNLIMITED.getDisplayName(),
////                SubscriptionPlan.UNLIMITED.getPriceDisplay()
////        );
////    }
//
//    // 🔥 ФОРМАТИРОВАНИЕ ДАТЫ ОБНОВЛЕНИЯ ЛИМИТОВ
//    private String formatNextResetDate() {
//        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
//        return nextMonth.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
//    }
//
////    private void cancelApplicationCreation(ProjectData data) {
////
////        applicationCreationService.cancelCreation(data.getChatId());
////
////        String text = """
////        ❌ <b>**СОЗДАНИЕ ОТКЛИКА ОТМЕНЕНО**</b>
////
////        <i>💡 Вы можете вернуться к проекту и создать отклик позже</i>
////        """;
////
////        Integer mainMessageId = getMainMessageId(data.getChatId());
////
////        editMessageWithHtml(data.getChatId(), mainMessageId, text, keyboardFactory.createToMainMenuKeyboard());
////
////        log.info("❌ Пользователь {} отменил создание отклика", data.getChatId());
////    }
//
////    public void withdrawApplication(ProjectData data, String applicationIdParam) {
////        try {
////            Long applicationId = Long.parseLong(applicationIdParam);
////
////            applicationService.withdrawApplication(applicationId, data.getChatId());
////
////            String successText = """
////                ↩️<b> **ОТКЛИК ОТОЗВАН** </b>
////
////                📨<i> Заявка успешно отозвана
////                👔 Заказчик уведомлен</i>
////                """;
////
////            InlineKeyboardMarkup keyboard = keyboardFactory.createToMainMenuKeyboard();
////
////
////
////            Integer mainMessageId = getMainMessageId(data.getChatId());
////            if (mainMessageId != null) {
////                editMessageWithHtml(data.getChatId(), mainMessageId, successText, keyboard);
////            } else {
////                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), successText, keyboard);
////                saveMainMessageId(data.getChatId(), newMessageId);
////            }
////
////            log.info("✅ Пользователь {} отозвал отклик {}", data.getChatId(), applicationId);
////        } catch (Exception e) {
////            log.error("❌ Ошибка отзыва отклика: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отзыва отклика: " + e.getMessage(), 5);
////        }
////    }
//
////    private void confirmWithdrawApplication(ProjectData data, String applicationIdParam) {
////        try {
////            Long applicationId = Long.parseLong(applicationIdParam);
////
////            // 🔥 ИСПОЛЬЗУЕМ DTO ВМЕСТО СУЩНОСТИ
////            ApplicationDto applicationDto = applicationService.getApplicationDtoById(applicationId);
////
////            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ОТКЛИКА
////            if (!applicationDto.getFreelancerChatId().equals(data.getChatId())) {
////                sendTemporaryErrorMessage(data.getChatId(), "❌ У вас нет доступа к этому отклику", 5);
////                return;
////            }
////
////            // 🔥 ПРОВЕРЯЕМ, ЧТО ОТКЛИК МОЖНО ОТОЗВАТЬ
////            if (applicationDto.getStatus() != UserRole.ApplicationStatus.PENDING) {
////                sendTemporaryErrorMessage(data.getChatId(),
////                        "❌ Нельзя отозвать отклик со статусом: " + getApplicationStatusDisplay(applicationDto.getStatus()), 5);
////                return;
////            }
////
////
////            // 🔥 ПОЛУЧАЕМ ДАННЫЕ ПРОЕКТА ЧЕРЕЗ СЕРВИС
////            String projectTitle = projectService.getProjectTitleById(applicationDto.getProjectId());
////
////            String warningText = """
////            <b>⚠️ **ПОДТВЕРЖДЕНИЕ ОТЗЫВА ОТКЛИКА**</b>
////
////            <blockquote>📋 *Проект:* %s
////            💰 *Ваш бюджет:* %.0f руб
////            ⏱️ *Ваш срок:* %d дней
////            📅 *Отправлен:* %s</blockquote>
////
////            🔴<b> *Внимание! </b>После отзыва:*
////            <i>• Отклик будет отмечен как отозванный
////            • Заказчик больше не увидит ваш отклик
////            • Вернуть отклик будет невозможно
////            • Использованный отклик не вернется в лимит</i>
////
////            ❓ <b>*Вы точно хотите отозвать этот отклик?*</b>
////            """.formatted(
////                    projectTitle,
////                    applicationDto.getProposedBudget(),
////                    applicationDto.getProposedDays(),
////                    applicationDto.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
////            );
////            InlineKeyboardMarkup keyboard = keyboardFactory.createWithdrawConfirmationKeyboard(applicationId);
////
////            Integer mainMessageId = getMainMessageId(data.getChatId());
////            if (mainMessageId != null) {
////                editMessageWithHtml(data.getChatId(), mainMessageId, warningText, keyboard);
////            } else {
////                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), warningText, keyboard);
////                saveMainMessageId(data.getChatId(), newMessageId);
////            }
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка подтверждения отзыва отклика: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка подтверждения отзыва", 5);
////        }
////    }
//
////    // 🔥 МЕТОД ДЛЯ ПОКАЗА ДЕТАЛЕЙ ОТКЛИКА
////    private void showApplicationDetails(ProjectData data, String applicationIdParam) {
////        try {
////            Long applicationId = Long.parseLong(applicationIdParam);
////            ApplicationDto application = applicationService.getApplicationDtoById(applicationId);
////
////            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ОТКЛИКА
////            if (application.getFreelancer().getId().equals(data.getChatId())) {
////                sendTemporaryErrorMessage(data.getChatId(), "❌ У вас нет доступа к этому отклику", 5);
////                return;
////            }
////
////            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ
////            deletePreviousMessages(data.getChatId());
////
////            String applicationText = formatApplicationDetails(application);
////            InlineKeyboardMarkup keyboard = keyboardFactory.createApplicationDetailsKeyboard(
////                    application.getId(),
////                    application.getStatus()
////            );
////
////            // 🔥 СОХРАНЯЕМ MESSAGE_ID ЕСЛИ ЕЩЁ НЕТ
////            if (getMainMessageId(data.getChatId()) == null) {
////                saveMainMessageId(data.getChatId(), data.getMessageId());
////            }
////
////            Integer mainMessageId = getMainMessageId(data.getChatId());
////
////            if (mainMessageId != null) {
////                editMessageWithHtml(data.getChatId(), mainMessageId, applicationText, keyboard);
////            } else {
////                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), applicationText, keyboard);
////                saveMainMessageId(data.getChatId(), newMessageId);
////            }
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа деталей отклика: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки информации об отклике", 5);
////        }
////    }
//
//    // 🔥 ФОРМАТИРОВАНИЕ ДЕТАЛЕЙ ОТКЛИКА
////    private String formatApplicationDetails(ApplicationDto application) {
////        ProjectDto project = application.getProject();
////
////        return """
////        <b>📋 **ДЕТАЛИ ВАШЕГО ОТКЛИКА**</b>
////
////        <blockquote><b>💼 *Проект:* %s</b>
////        <b>👔 *Заказчик:* @%s</b>
////        <b>⭐ *Рейтинг заказчика:* %.1f/5.0</b>
////
////        <b>💰 *Ваше предложение по бюджету:* %.0f руб</b>
////        <b>💵 *Бюджет проекта:* %.0f руб</b>
////
////        <b>⏱️ *Ваш срок выполнения:* %d дней</b>
////        <b>📅 *Срок проекта:* %d дней</b>
////
////        <b>📅 *Отклик отправлен:* %s</b>
////        <b>📊 *Статус:* %s</b>
////        <b>%s</b>
////        <b>📝 *Ваше сопроводительное письмо:*</b>
////        <i>%s</i>
////
////        <b>🛠️ *Требуемые навыки:*</b>
////        <u>%s</u></blockquote>
////        """.formatted(
////                project.getTitle(),
////                project.getCustomerUserName() != null ? project.getCustomerUserName() : "скрыт",
////                project.getCustomerRating(),
////                application.getProposedBudget(),
////                project.getBudget(),
////                application.getProposedDays(),
////                project.getEstimatedDays(),
////                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
////                getApplicationStatusDisplay(application.getStatus()),
////                getApplicationStatusDetails(application),
////                application.getCoverLetter(),
////                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны"
////        );
////    }
//
////    private String getApplicationStatusDisplay(UserRole.ApplicationStatus applicationStatus) {
////        return switch (applicationStatus) {
////            case PENDING -> "Ожидает рассмотрения";
////            case ACCEPTED -> "Принят заказчиком";
////            case REJECTED -> "Отклонен заказчиком";
////            case WITHDRAWN -> "Отозван исполнителем";
////        };
////    }
//
////    // 🔥 ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ О СТАТУСЕ
////    private String getApplicationStatusDetails(ApplicationDto application) {
////        if (application.getReviewedAt() != null && application.getCustomerComment() != null) {
////            return "💬 *Комментарий заказчика:* " + application.getCustomerComment() + "\n";
////        }
////        return "";
////    }
//
////    // 🔥 ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ЭКРАНИРОВАНИЯ HTML
////    private String escapeHtml(String text) {
////        if (text == null) return "";
////        return text.replace("&", "&amp;")
////                .replace("<", "&lt;")
////                .replace(">", "&gt;")
////                .replace("\"", "&quot;")
////                .replace("'", "&#39;");
////    }
//
//    public boolean isCreatingApplication(Long chatId) {
//        return applicationCreationService.isCreatingApplication(chatId);
//    }
//
////    public List<Integer> renderFreelancerApplicationsPage(List<Long> pageApplicationIds, PaginationContext context) {
////        Long chatId = context.chatId();
////        List<Integer> messageIds = new ArrayList<>();
////
////        List<ApplicationDto> pageApplications = applicationService.getApplicationsByIds(pageApplicationIds);
////
////        // Заголовок
////        String headerText = String.format("""
////            📨 <b>МОИ ОТКЛИКИ</b>
////
////            <i>Найдено %d откликов. Страница %d из %d</i>
////            """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());
////
////        Integer headerId = sendHtmlMessageReturnId(chatId, headerText, null);
////        if (headerId != null) messageIds.add(headerId);
////
////        for (int i = 0; i < pageApplications.size(); i++) {
////            ApplicationDto application = pageApplications.get(i);
////            String applicationCardText = formatApplicationPreview(application, (context.currentPage() * context.pageSize()) + i + 1);
////
////            InlineKeyboardMarkup keyboard = keyboardFactory.createApplicationItemKeyboard(application.getId(), application.getStatus());
////
////            Integer cardId = sendHtmlMessageReturnId(chatId, applicationCardText, keyboard);
////            if (cardId != null) messageIds.add(cardId);
////        }
////
////        // Пагинация
////        if (context.getTotalPages() > 1) {
////            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboardForContext(context);
////
////            Integer navId = sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
////            if (navId != null) messageIds.add(navId);
////        }
////
////        return messageIds;
////    }
//
//    // 🔥 МЕТОД РЕНДЕРИНГА ДЛЯ ОТКЛИКОВ НА ПРОЕКТ (ИСПОЛЬЗУЕТ DTO)
////    private List<Integer> renderProjectApplicationsPage(List<Long> pageApplicationIds, PaginationContext context) {
////        Long chatId = context.chatId();
////        List<Integer> messageIds = new ArrayList<>();
////
////        // 🔥 ПОЛУЧАЕМ DTO ВМЕСТО ПОЛНЫХ СУЩНОСТЕЙ
////        List<ApplicationDto> pageApplications = applicationService.getApplicationsByIds(pageApplicationIds);
////
////        // Заголовок
////        String headerText = String.format("""
////            📨 <b>ОТКЛИКИ НА ПРОЕКТ</b>
////
////            <i>Найдено %d откликов. Страница %d из %d</i>
////            """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());
////
////        Integer headerId = sendHtmlMessageReturnId(chatId, headerText, keyboardFactory.createBackButton());
////        if (headerId != null) messageIds.add(headerId);
////
////        // Карточки откликов (используем DTO)
////        for (int i = 0; i < pageApplications.size(); i++) {
////            ApplicationDto application = pageApplications.get(i);
////            String applicationText = formatApplicationForCustomer(application, (context.currentPage() * context.pageSize()) + i + 1);
//////            InlineKeyboardMarkup keyboard = keyboardFactory.createApplicationResponseKeyboard(application.getId());
////
////            Integer cardId = sendHtmlMessageReturnId(chatId, applicationText, null);
////            if (cardId != null) messageIds.add(cardId);
////        }
////
////        // Пагинация
////        if (context.getTotalPages() > 1) {
////            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboardForContext(context);
////
////            Integer navId = sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
////            if (navId != null) messageIds.add(navId);
////        }
////
////        return messageIds;
////    }
//
////    private String formatApplicationForCustomer(ApplicationDto application, int number) {
////        return """
////            <b>📨 Отклик #%d</b>
////
////            <blockquote><b>👨‍💻 Исполнитель:</b> %s
////            <b>💰 Предложил:</b> %.0f руб
////            <b>⏱️ Срок:</b> %d дней
////            <b>📅 Отправлен:</b> %s
////            <b>⭐ Рейтинг:</b> %.1f/5.0
////
////            <b>📝 Сообщение:</b>
////            <i>%s</i></blockquote>
////            """.formatted(
////                number,
////                application.getFreelancer().getUserName() != null ?
////                        "@" + application.getFreelancer().getUserName() : "Пользователь",
////                application.getProposedBudget(),
////                application.getProposedDays(),
////                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
////                application.getFreelancer().getRating(),
////                application.getCoverLetter().length() > 200 ?
////                        application.getCoverLetter().substring(0, 200) + "..." :
////                        application.getCoverLetter()
////        );
////    }
//
////    public void showProjectApplications(Long chatId, String projectIdStr, Integer messageId) {
////        try {
////            Long projectId = Long.parseLong(projectIdStr);
////
////            // 🔥 ПОЛУЧАЕМ ID ОТКЛИКОВ ВМЕСТО ПОЛНЫХ СУЩНОСТЕЙ
////            List<Long> applicationIds = applicationService.getProjectApplicationIds(projectId);
////
////            if (applicationIds.isEmpty()) {
////                showNoApplicationsMessage(chatId, messageId, userService.getUserRole(chatId));
////                return;
////            }
////
////            // 🔥 ИСПОЛЬЗУЕМ PAGINATION MANAGER С ID
////            paginationManager.renderIdBasedPage(
////                    chatId,
////                    PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY,
////                    applicationIds,
////                    "APPLICATION",
////                    "init",
////                    APPLICATIONS_PER_PAGE,
////                    this::renderProjectApplicationsPage
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа откликов на проект: {}", e.getMessage());
////            sendTemporaryErrorMessage(chatId, "Ошибка загрузки откликов", 5);
////        }
////    }
//
//
//
////    private String formatApplicationPreview(ApplicationDto application, int number) {
////        String projectTitle = projectService.getProjectTitleById(application.getProjectId());
////
////        return """
////        <b>📨 **Отклик #%d**</b>
////
////        <blockquote><b>💼 *Проект:* %s</b>
////        <b>💰 *Ваше предложение:* %.0f руб</b>
////        <b>⏱️ *Срок:* %d дней</b>
////        <b>📅 *Отправлен:* %s</b>
////        <b>📊 *Статус:* %s</b>
////
////        <b>📝 *Ваше сообщение:*</b>
////        <i>%s</i></blockquote>
////        """.formatted(
////                number,
////                projectTitle,
////                application.getProposedBudget(),
////                application.getProposedDays(),
////                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
////                getApplicationStatusDisplay(application.getStatus()),
////                application.getCoverLetter().length() > 150 ?
////                        application.getCoverLetter().substring(0, 150) + "..." :
////                        application.getCoverLetter()
////        );
////    }
//
//    /**
//     * Возвращает функцию рендеринга для использования в CallbackHandler/PaginationManager.
//     * 🔥 Использует универсальный тип BiFunction<?, ?, ?> для совместимости с внешними вызовами.
//     */
//
////    // 🔥 ОБРАБОТКА ПАГИНАЦИИ ДЛЯ APPLICATION HANDLER
////    private void handleApplicationPagination(ProjectData data, String parameter) {
////        try {
////            // Формат: "next:my_applications:APPLICATION" или "next:project_applications:APPLICATION"
////            String[] parts = parameter.split(":");
////            if (parts.length < 3) return;
////
////            String direction = parts[0];
////            String contextKey = parts[1];
////            String entityType = parts[2];
////
////            // 🔥 ОПРЕДЕЛЯЕМ РЕНДЕРЕР ДЛЯ КОНТЕКСТА
////            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = null;
////
////            if (PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY.equals(contextKey)) {
////                renderer = this::renderFreelancerApplicationsPage;
////            } else if (PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY.equals(contextKey)) {
////                renderer = this::renderProjectApplicationsPage;
////            }
////
////            if (renderer == null) {
////                log.error("❌ Renderer not found for application context: {}", contextKey);
////                return;
////            }
////
////            // 🔥 ВЫЗЫВАЕМ PAGINATION MANAGER
////            paginationManager.renderIdBasedPage(
////                    data.getChatId(),
////                    contextKey,
////                    null, // ID уже в контексте
////                    entityType,
////                    direction,
////                    APPLICATIONS_PER_PAGE,
////                    renderer
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка пагинации откликов: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
////        }
////    }
//
//    // 🔥 3. Отображение пустого списка (с учетом роли)
////    private void showNoApplicationsMessage(Long chatId, Integer messageId, UserRole role) {
////        String text;
////        if (role == UserRole.FREELANCER) {
////            text = """
////                📨 <b>**МОИ ОТКЛИКИ**</b>
////
////                📭<i> Вы еще не откликались на проекты</i>
////
////                💡 *Как найти проекты:*
////                • Используйте поиск проектов
////                • Изучите требования заказчиков
////                • Отправляйте качественные отклики
////                """;
////        } else if (role == UserRole.CUSTOMER) {
////            text = """
////                📭 <b>**ОТКЛИКОВ НЕТ**</b>
////
////                💡 <i>На ваши проекты еще никто не откликнулся, либо у вас нет активных проектов.</i>
////                """;
////        } else {
////            text = "📭 Ничего не найдено";
////        }
////
////        // Предполагаем, что createBackButton возвращает кнопку "Назад"
////        editMessageWithHtml(chatId, messageId, text, keyboardFactory.createBackButton());
////    }
//
//    public int getApplicationsPerPage() {
//        return APPLICATIONS_PER_PAGE;
//    }
//
//    // 🔥 ГЕТТЕРЫ ДЛЯ CallbackHandler
//    public String getFreelancerApplicationsContextKey() {
//        return PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY;
//    }
//
//    public String getProjectApplicationsContextKey() {
//        return PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY;
//    }
//
////    public BiFunction<List<Long>, PaginationContext, List<Integer>> getFreelancerApplicationsRenderer() {
////        return this::renderFreelancerApplicationsPage;
////    }
////
////    public BiFunction<List<Long>, PaginationContext, List<Integer>> getProjectApplicationsRenderer() {
////        return this::renderProjectApplicationsPage;
////    }
//}
