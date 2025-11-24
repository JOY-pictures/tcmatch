//package com.tcmatch.tcmatch.bot.handlers;
//
//import com.tcmatch.tcmatch.bot.BotExecutor;
//import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
//import com.tcmatch.tcmatch.model.Project;
//import com.tcmatch.tcmatch.model.dto.*;
//import com.tcmatch.tcmatch.model.enums.UserRole;
//import com.tcmatch.tcmatch.service.*;
//import com.tcmatch.tcmatch.util.PaginationContextKeys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.function.BiFunction;
//import java.util.stream.Collectors;
//
//@Component
//@Slf4j
//public class ProjectsHandler extends BaseHandler {
//
//    private final PaginationManager paginationManager;
//    private final ProjectViewService projectViewService;
//    private final ProjectService projectService;
//    private final ApplicationService applicationService;
//    private final ProjectSearchService projectSearchService;
//    private final ApplicationHandler applicationHandler;
//    private final RoleBasedMenuService roleBasedMenuService;
//    private final UserService userService;
//    private final ProjectCreationService projectCreationService;
//
//    private static final int PROJECTS_PER_PAGE = 5;
//    private static final int APPLICATIONS_PER_PAGE = 3;
//
/// /    private static final String FAVORITES_CONTEXT_KEY = "favorites";
//
////    private static final String APPLICATIONS_CONTEXT_KEY = "applications";
//
////    private static final String MY_PROJECTS_CONTEXT_KEY = "my_projects";
//
//    private static final String SEARCH_STATE_KEY = "search_request_data"; // Ключ для хранения DTO
//    private static final String SEARCH_ACTION_FILTER = "filter";
////    private static final String SEARCH_CONTEXT_KEY = "project_search"; // 👈 Уникальный ключ для поиска
//
//    private int delaySeconds;
//
//    // 🔥 MAP ДЛЯ ХРАНЕНИЯ ID СООБЩЕНИЙ С ПРОЕКТАМИ
//
//    public ProjectsHandler(KeyboardFactory keyboardFactory, PaginationManager paginationManager, ProjectViewService projectViewService,
//                           ProjectService projectService, ApplicationService applicationService,
//                           ProjectSearchService projectSearchService, ApplicationHandler applicationHandler,
//                           UserSessionService userSessionService, RoleBasedMenuService roleBasedMenuService,
//                           UserService userService, BotExecutor botExecutor, ProjectCreationService projectCreationService) {
//        super(botExecutor, keyboardFactory, userSessionService);
//        this.paginationManager = paginationManager;
//        this.projectViewService = projectViewService;
//        this.projectService = projectService;
//        this.applicationService = applicationService;
//        this.projectSearchService = projectSearchService;
//        this.applicationHandler = applicationHandler;
//        this.roleBasedMenuService = roleBasedMenuService;
//        this.userService = userService;
//        this.projectCreationService = projectCreationService;
//    }
//
//    @Override
//    public boolean canHandle(String actionType, String action) {
//        return "project".equals(actionType) || "project_creation".equals(action);
//    }
//
//    @Override
//    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
//        ProjectData data = new ProjectData(chatId, messageId, userName, null, action, parameter);
//
//        switch (action) {
//            case "project_creation":
//                String[] parts = action.split(":", 2);
//                if (parts.length > 1) {
//                    handleProjectCreationCallback(data, parts[1], parameter);
//                }
//                return;
////            case "menu":
////                showProjectsMenu(data);
////                break;
//            // 🔥 НОВЫЙ БЛОК: ОБРАБОТКА ПАГИНАЦИИ
//            case "next":
//            case "prev":
//                handlePagination(data, parameter);
//                break;
////            case "my_projects":
////                showMyProjectsMenu(data);
////                break;
////            case "my_list":
////                showMyProjectsList(data, parameter);
////                break;
////            case "favorites":
////                handleFavorites(data);
////                break;
////            case "favorite":
////                handleFavorite(data, parameter);
////                break;
////            case "applications":
////                if (parameter != null) {
////                    // 🔥 ОТКЛИКИ НА КОНКРЕТНЫЙ ПРОЕКТ (projects:applications:123)
////                    showProjectApplications(data, parameter);
////                } else {
////                    // 🔥 МОИ ОТКЛИКИ КАК ИСПОЛНИТЕЛЬ (projects:applications)
////                    handleFreelancerApplications(data);
////                }
////                break;
////            case "active":
////                showActiveProjects(data);
////                break;
////            case "search":
////                showProjectSearch(data);
////                break;
////            case "details":
////                showProjectDetail(data);
////                break;
////            case "filter":
////                handleProjectFilterAction(data, parameter);
////                break;
//            case "pagination":
//                handlePagination(data, parameter);
//                break;
//            case "create":
//                startProjectCreation(data);
//                break;
//            default:
//                log.warn("❌ Unknown projects action: {}", action);
//        }
//    }
//
//    public void showProjectsMenu(ProjectData data) {
//        String text = """
//            💼 <b>**РАЗДЕЛ ПРОЕКТОВ TCMatch**</b>
//
//            <i>Выберите нужный раздел:</i>
//            """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectsMenuKeyboard(data.getChatId() );
//        editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboard);
//    }
//
//    public void showMyProjectsMenu(ProjectData data) {
//        UserRole userRole = roleBasedMenuService.getUserRole(data.getChatId());
//
//        if (userRole == UserRole.CUSTOMER) {
//            String text = """
//                👔 <b>**МОИ ПРОЕКТЫ**</b>
//
//                <i>Управление вашими проектами:</i>
//                """;
//            InlineKeyboardMarkup keyboard = roleBasedMenuService.createMyProjectsMenu(data.getChatId());
//            editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboard);
//
//
//
//
//
//
//
//
//
//
//
//
//        } else {
//            String text = """
//                👨‍💻 <b>**УПРАВЛЕНИЕ ЗАКАЗАМИ**<b>
//
//                📊 <u>Этот раздел доступен только заказчикам</u>
//
//                💡 <i>Для исполнителей доступны:
//                • ⚙️ Выполняемые - ваши активные заказы
//                • 📨 Откликнутые - проекты, куда вы откликнулись
//                • 🔍 Поиск проектов - находите новые проекты</i>
//                """;
//            InlineKeyboardMarkup keyboard = roleBasedMenuService.createMyProjectsMenu(data.getChatId());
//            editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboard);
//        }
//    }
//
//    private void showMyProjectsList(ProjectData data, String statusFilter) {
//
//        try {
//            Long chatId = data.getChatId();
//
//            // 🔥 ПОЛУЧАЕМ ID ПРОЕКТОВ ЗАКАЗЧИКА
//            List<Long> projectIds = projectService.getProjectIdsByCustomerChatId(chatId);
//
//            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ (если были)
//            deletePreviousMessages(chatId);
//
//            // 🔥 ГЛАВНОЕ СООБЩЕНИЕ С КНОПКОЙ "СОЗДАТЬ ПРОЕКТ"
//            String mainText = """
//            👔 <b>**МОИ ПРОЕКТЫ**</b>
//
//            💼 <i>Управление вашими проектами</i>
//            """;
//            InlineKeyboardMarkup mainKeyboard = keyboardFactory.createCustomerProjectsMainKeyboard();
//            if (projectIds.isEmpty()) {
//                String text = """
//
//                📭 <b>ПРОЕКТЫ НЕ НАЙДЕНЫ</b>
//
//                💡<u> Создайте первый проект чтобы найти исполнителя</u>
//                """;
//                editMessageWithHtml(chatId, data.getMessageId(), mainText + text, mainKeyboard);
//                return;
//            }
//
//
//
//            // 🔥 СОХРАНЯЕМ MESSAGE_ID ЕСЛИ ЕЩЁ НЕТ
//            if (getMainMessageId(chatId) == null) {
//                saveMainMessageId(chatId, data.getMessageId());
//            }
//
//
//
//            // 🔥 КЛАВИАТУРА ДЛЯ ГЛАВНОГО СООБЩЕНИЯ
//
//            editMessageWithHtml(chatId, getMainMessageId(chatId), mainText, mainKeyboard);
//
//            // 🔥 ЗАПУСКАЕМ ПАГИНАЦИЮ ЧЕРЕЗ PAGINATION MANAGER
//            paginationManager.renderIdBasedPage(
//                    chatId,
//                    "customer_projects",     // контекст для пагинации
//                    projectIds,              // ID проектов
//                    "PROJECT",               // тип сущности
//                    "init",                  // направление
//                    PROJECTS_PER_PAGE,       // размер страницы
//                    this::renderCustomerProjectsPage  // рендерер
//            );
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа списка проектов: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки проектов", 5);
//        }
//
////        try {
////            List<Long> projectIds = projectService.getUserProjectIds(data.getChatId());
////
////
////            if (projectIds.isEmpty()) {
////                String text = """
////                    📭 <b>**ПРОЕКТЫ НЕ НАЙДЕНЫ**</b>
////
////                    💡<i> Создайте первый проект чтобы найти исполнителя</i>
////                    """;
////                editMessageWithHtml(data.getChatId(), data.getMessageId(), text,
////                        keyboardFactory.createBackToMyProjectsKeyboard());
////                return;
////            }
////
////            userSessionService.putToContext(data.getChatId(), "my_projects_list", projectIds);
////            userSessionService.putToContext(data.getChatId(), "my_projects_page", 0);
////            userSessionService.putToContext(data.getChatId(), "my_projects_filter", statusFilter);
////
//////            showCustomerProjectsPage(data, projectIds, 0, statusFilter);
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа списка проектов: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки проектов", 5);
////        }
//    }
//
//    private void handleFavorites(ProjectData data) {
//        try {
//            Long chatId = data.getChatId();
//
//            // 🔥 ПОЛУЧАЕМ ТОЛЬКО ID
//            List<Long> favoriteIds = projectService.getFavoriteProjectIds(chatId);
//
//
//            if (favoriteIds.isEmpty()) {
//                String text = """
//                        ⭐ <b>**ИЗБРАННЫЕ ПРОЕКТЫ**</b>
//
//                        📭 <i>У вас пока нет избранных проектов</i>
//
//                        💡<u> *Как добавить в избранное:*</u>
//                        • <i>Находите интересный проект в поиске
//                        • Нажимайте кнопку "⭐ В избранное"
//                        • Возвращайтесь к нему позже</i>
//                        """;
//                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
//                return;
//            }
//
//            paginationManager.renderIdBasedPage(
//                    chatId,
//                    "favorites",           // контекст
//                    favoriteIds,           // ID проектов
//                    "PROJECT",             // тип сущности
//                    "init",
//                    PROJECTS_PER_PAGE,
//                    this::renderFavoritesPage  // рендерер
//            );
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа избранных: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки избранных", 5);
//        }
//    }
//
//    private void showFavoritesPage(ProjectData data, List<Project> allProjects, int page) {
//        Long chatId = data.getChatId();
//        int totalCount = allProjects.size();
//
//        deletePreviousMessages(chatId);
//
//        int start = page * PROJECTS_PER_PAGE;
//        int end = Math.min(start + PROJECTS_PER_PAGE, totalCount);
//        List<Project> pageProjects = allProjects.subList(start, end);
//
//        String headerText = String.format("""
//                ⭐<b> **ИЗБРАННЫЕ ПРОЕКТЫ**</b>
//
//                <i>Найдено %d проектов.</i>
//                """,
//                totalCount);
//        editMessageWithHtml(chatId, userSessionService.getMainMessageId(chatId), headerText, keyboardFactory.createBackButton());
//        int i = 0;
//        for (Project project : pageProjects) {
//            i++;
//            String projectText = formatProjectPreview(project, i + 1);
//
//            // 🔥 УПРОЩЕННАЯ КЛАВИАТУРА - ТОЛЬКО "ДЕТАЛИ"
//            InlineKeyboardMarkup projectKeyboard = keyboardFactory.createProjectPreviewKeyboard(project.getId());
//
//            Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), projectText, projectKeyboard);
//            if (newMessageId != null) {
//                userSessionService.addTemporaryMessageId(chatId, newMessageId);
//            }
//        }
//
//        Integer paginationMsgId = sendHtmlMessageReturnId(chatId, "<i>📄 **СТРАНИЦА %d ИЗ %d**</i>".formatted(page + 1, (int) Math.ceil((double) totalCount / PROJECTS_PER_PAGE)),
//                keyboardFactory.createFavoritesPaginationKeyboard(page, totalCount, PROJECTS_PER_PAGE));
//
//        userSessionService.addTemporaryMessageId(chatId, paginationMsgId);
//    }
//
//    private void showCustomerProjectsPage(ProjectData data, List<Project> projects, int page, String filter) {
//        int pageSize = 3;
//        int totalPages = (int) Math.ceil((double) projects.size() / pageSize);
//        int startIndex = page * pageSize;
//        int endIndex = Math.min(startIndex + pageSize, projects.size());
//
//        String filterDisplay = getFilterDisplay(filter);
//
//        StringBuilder text = new StringBuilder("""
//            👔 <b>**ВАШИ ПРОЕКТЫ**</b>
//            """.formatted(filterDisplay, page + 1, totalPages));
//
//        for (int i = startIndex; i < endIndex; i++) {
//            Project project = projects.get(i);
//            text.append("""
//
//                %s%s
//                💰 %.0f руб | ⏱️ %d дн. | %s
//                👀 %d просмотров | 📨 %d откликов
//                """.formatted(
//                    getProjectStatusIcon(project.getStatus()),
//                    project.getTitle(),
//                    project.getBudget(),
//                    project.getEstimatedDays(),
//                    getProjectStatusDisplay(project.getStatus()),
//                    project.getViewsCount(),
//                    project.getApplicationsCount()
//            ));
//        }
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createCustomerProjectsListKeyboard(
//                projects, page, totalPages, filter);
//
//        editMessageWithHtml(data.getChatId(), data.getMessageId(), text.toString(), keyboard);
//    }
//
//    // 🔥 ОТКЛИКИ НА ПРОЕКТ (для заказчика)
//    private void showProjectApplications(ProjectData data, String projectId) {
//        try {
//            Long projectIdLong = Long.parseLong(projectId);
//            List<ApplicationDto> applications = applicationService.getProjectApplicationDTOs(projectIdLong);
//
//            if (applications.isEmpty()) {
//                String text = """
//                    📭 <b>**ОТКЛИКОВ НЕТ**</b>
//
//                    💡 <i>На ваш проект еще никто не откликнулся</i>
//                    """;
//                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
//                return;
//            }
//
//            showApplicationsForProject(data, applications, projectIdLong);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа откликов на проект: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки откликов", 5);
//        }
//    }
//
//    // 🔥 ОТОБРАЖЕНИЕ ОТКЛИКОВ НА ПРОЕКТ
//    private void showApplicationsForProject(ProjectData data, List<ApplicationDto> applications, Long projectId) {
//        StringBuilder text = new StringBuilder("""
//            📨 <b>**ОТКЛИКИ НА ПРОЕКТ**</b>
//
//            """);
//
//        for (int i = 0; i < Math.min(applications.size(), 10); i++) {
//            ApplicationDto app = applications.get(i);
//            String freelancerUserName = app.getFreelancer().getUserName();
//            text.append("""
//                %d. 👨‍💻 *%s*
//                   💰 Предложил: %.0f руб
//                   ⏱️ Срок: %d дней
//               📊 Рейтинг: ⭐ %.1f
//                   📅 Отправлен: %s
//
//                """.formatted(
//                    i + 1,
//                     freelancerUserName != null ?
//                            "@" + freelancerUserName : "Пользователь",
//                    app.getProposedBudget(),
//                    app.getProposedDays(),
//                    app.getFreelancer().getProfessionalRating(),
//                    app.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
//            ));
//        }
//
//        if (applications.size() > 10) {
//            text.append("\n📊 ... и еще ").append(applications.size() - 10).append(" откликов");
//        }
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectApplicationsKeyboard(projectId);
//        editMessageWithHtml(data.getChatId(), data.getMessageId(), text.toString(), keyboard);
//    }
//
//    // 🔥 ОБНОВЛЯЕМ showProjectDetail - добавляем поддержку applicationId
//    public void showProjectDetail(ProjectData data) {
//        try {
//            Long projectId;
//            String parameter = data.getParameter();
//
//            // 🔥 ПРОВЕРЯЕМ - ПЕРЕДАН ID ПРОЕКТА ИЛИ ID ОТКЛИКА?
//            if (parameter.startsWith("app_")) {
//                // 🔥 ЕСЛИ ПЕРЕДАН ID ОТКЛИКА (app_123) - ПОЛУЧАЕМ ID ПРОЕКТА
//                Long applicationId = Long.parseLong(parameter.replace("app_", ""));
//                projectId = applicationService.getProjectIdByApplicationId(applicationId);
//            } else {
//                // 🔥 ЕСЛИ ПЕРЕДАН ОБЫЧНЫЙ ID ПРОЕКТА
//                projectId = Long.parseLong(parameter);
//            }
//
//            ProjectDto project = projectService.getProjectDtoById(projectId)
//                    .orElseThrow(() -> new RuntimeException("Проект не найден"));
//
//            deletePreviousMessages(data.getChatId());
//
//            // 🔥 РЕГИСТРИРУЕМ ПРОСМОТР ТОЛЬКО ЗДЕСЬ - КОГДА ПОЛЬЗОВАТЕЛЬ ДЕЙСТВИТЕЛЬНО СМОТРИТ ПРОЕКТ
//            projectViewService.registerProjectView(data.getChatId(), projectId);
//
//            String projectText = formatProjectDetails(project);
//
//            boolean canApply = roleBasedMenuService.canUserApplyToProjects(data.getChatId()) &&
//                    !roleBasedMenuService.isProjectOwner(data.getChatId(), project.getCustomerChatId());
//
//            InlineKeyboardMarkup keyboard = roleBasedMenuService.createProjectDetailsKeyboard(
//                    data.getChatId(), projectId, canApply);
//
//            Integer mainMessageId = getMainMessageId(data.getChatId());
//
//            if (mainMessageId != null) {
//                editMessageWithHtml(data.getChatId(), mainMessageId, projectText, keyboard);
//            } else {
//                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), projectText, keyboard);
//                saveMainMessageId(data.getChatId(), newMessageId);
//            }
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа деталей проекта: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки информации о проекте", 5);
//        }
//    }
//
//    // 🔥 ПАГИНАЦИЯ "МОИХ ПРОЕКТОВ"
//    private void handleMyProjectsPagination(ProjectData data, String parameter) {
//        try {
//            String[] parts = parameter.split(":");
//            String direction = parts[0];
//            String filter = parts[2];
//
//            List<Project> projects = userSessionService.getFromContext(data.getChatId(),
//                    "my_projects_list", List.class);
//            Integer currentPage = userSessionService.getFromContext(data.getChatId(),
//                    "my_projects_page", Integer.class);
//
//            if (projects == null || currentPage == null) {
//                showMyProjectsList(data, filter);
//                return;
//            }
//
//            int totalPages = (int) Math.ceil((double) projects.size() / 3);
//            int newPage = currentPage;
//
//            if ("next".equals(direction) && currentPage < totalPages - 1) {
//                newPage = currentPage + 1;
//            } else if ("prev".equals(direction) && currentPage > 0) {
//                newPage = currentPage - 1;
//            }
//
//            userSessionService.putToContext(data.getChatId(), "my_projects_page", newPage);
//            showCustomerProjectsPage(data, projects, newPage, filter);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка пагинации моих проектов: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
//        }
//    }
//
//    private void handleFavoritesPagination (ProjectData data, String parameter) {
//        try {
//            // Parameter формат: "next:favorites" или "prev:favorites"
//            String[] parts = parameter.split(":");
//            String direction = parts[0];
//
//// 1. Извлекаем данные из сессии
//            // 🔥 ВАЖНО: Мы извлекаем List<Project> через List.class, используя обходной путь
//            // для List<Project> через сырой List.class или используя кастомный TypeReference.
//            List<Project> allProjects = userSessionService.getFromContext(data.getChatId(),
//                    "favorites_list", List.class);
//            Integer currentPage = userSessionService.getFromContext(data.getChatId(),
//                    "favorites_page", Integer.class);
//
//            if (allProjects == null || currentPage == null) {
//                // Если данные сессии потеряны
//                handleFavorites(data);
//                return;
//            }
//
//            // 2. Расчет новой страницы
//            int totalPages = (int) Math.ceil((double) allProjects.size() / PROJECTS_PER_PAGE);
//            int newPage = currentPage;
//
//            if ("next".equals(direction) && currentPage < totalPages - 1) {
//                newPage = currentPage + 1;
//            } else if ("prev".equals(direction) && currentPage > 0) {
//                newPage = currentPage - 1;
//            } else {
//                newPage = currentPage;
//            }
//
//            // 3. Обновляем сессию и рендерим
//            userSessionService.putToContext(data.getChatId(), "favorites_page", newPage);
//            showFavoritesPage(data, allProjects, newPage);
//        } catch (Exception e) {
//            log.error("❌ Ошибка пагинации избранного: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "❌ Ошибка при переключении страницы.", 5);
//        }
//    }
//
////    private void handleFreelancerApplications(ProjectData data) {
////        try {
////            Long chatId = data.getChatId();
////            // 1. Получаем ВЕСЬ активный список избранного (для хранения в сессии)
////            // 🔥 Предполагаем, что projectService.getAllActiveFavoriteProjects(chatId) существует
////            List<Long> applicationIds = applicationService.getUserApplicationIds(chatId);
////
////            if (applicationIds.isEmpty()) {
////                String text = """
////                        📨 <b>**МОИ ОТКЛИКИ**</b>
////
////                        📭<i> Вы еще не откликались на проекты</i>
////
////                        💡 *Как найти проекты:*
////                        • Используйте поиск проектов
////                        • Изучите требования заказчиков
////                        • Отправляйте качественные отклики
////                        """;
////                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
////                return;
////            }
////
////            // 2. 🔥 ДЕЛЕГИРОВАНИЕ: Передаем полный список и функцию рендеринга
////            paginationManager.renderIdBasedPage(
////                    chatId,
////                    APPLICATIONS_CONTEXT_KEY, // Контекст
////                    applicationIds,
////                    "APPLICATION",
////                    "init",          // Инициализация
////                    APPLICATIONS_PER_PAGE,
////                    this::renderFreelancerApplicationsPage
////            );// Функция, которая умеет рисовать страницу
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа избранных проектов: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки избранных проектов", 5);
////        }
////    }
//
//    public void showFavorites(ProjectData data) {
//    }
//
//    private void handleFavorite(ProjectData data, String parameter) {
//        Long chatId = data.getChatId();
//        // Parameter format: "add:123" или "remove:456"
//        String[] parts = parameter.split(":");
//
//        if (parts.length < 2) {
//            log.warn("❌ Некорректный параметр для избранного: {}", parameter);
//            return;
//        }
//
//        String actionType = parts[0]; // "add" или "remove"
//        Long projectId;
//
//        try {
//            projectId = Long.parseLong(parts[1]);
//        } catch (NumberFormatException e) {
//            log.error("❌ Некорректный ID проекта '{}' для избранного у пользователя {}", parts[1], chatId);
//            // Отправка уведомления пользователю
//            sendTemporaryErrorMessage(chatId, "❌ Произошла ошибка с ID проекта.", 5);
//            return;
//        }
//
//        try {
//            if ("add".equals(actionType)) {
//                userService.addFavoriteProject(chatId, projectId);
//                log.warn("Пользователь {} добавил в избранное проект {}", chatId, projectId);
//            } else if ("remove".equals(actionType)) {
//                userService.removeFavoriteProject(chatId, projectId);
//                log.warn("Пользователь {} удалил из избранного проект {}", chatId, projectId);
//            } else {
//                log.warn("❌ Неизвестный тип действия для избранного: {}", actionType);
//            }
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка при изменении избранного для {} ({}): {}", chatId, projectId, e.getMessage());
//        }
//
//        // 1. УВЕДОМЛЕНИЕ: Отправляем временное уведомление (используем ваш существующий метод)
//
//        // 2. ОБНОВЛЕНИЕ UI: Перерисовываем детальную карточку проекта.
//        // Устанавливаем в data.parameter ID проекта, чтобы showProjectDetail(data) знал, какой проект загрузить.
//        data.setParameter(String.valueOf(projectId));
//
//        // Поскольку мы только что обновили статус избранного,
//        // нам нужно, чтобы карточка детализации обновилась (самый надежный способ - повторный вызов).
//        // 🔥 Важно: showProjectDetail должен использовать messageId из data для редактирования.
//        showProjectDetail(data);
//    }
//
////    public void showMyApplications(ProjectData data) {
////        try {
////            // 🔥 РЕАЛЬНАЯ ЛОГИКА - получение откликов пользователя
////            List<Application> userApplications = applicationService.getUserApplications(data.getChatId());
////
////            if (userApplications.isEmpty()) {
////                String text = """
////                        📨 <b>**ОТКЛИКНУТНЫЕ ПРОЕКТЫ**</b>
////
////                        📭<i> Вы еще не откликались на проекты</i>
////
////                        💡 *Как найти проекты:*
////                        • Используйте поиск проектов
////                        • Изучите требования заказчиков
////                        • Отправляйте качественные отклики
////                        """;
////                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
////                return;
////            }
////
////            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ С ОТКЛИКАМИ
////            deletePreviousMessages(data.getChatId());
////
////            // 🔥 СОХРАНЯЕМ MESSAGE_ID ЕСЛИ ЕЩЁ НЕТ
////            if (getMainMessageId(data.getChatId()) == null) {
////                saveMainMessageId(data.getChatId(), data.getMessageId());
////            }
////
////// 🔥 СОХРАНЯЕМ ДЛЯ ПАГИНАЦИИ
////            userSessionService.putToContext(data.getChatId(), "my_applications_list", userApplications);
////            userSessionService.putToContext(data.getChatId(), "my_applications_page", 0);
////
////            // 🔥 ПОКАЗЫВАЕМ ПЕРВУЮ СТРАНИЦУ
////            showApplicationsPage(data, userApplications, 0);
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа откликов: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки ваших откликов", 5);
////        }
////    }
//
////    private void showApplicationsPage(ProjectData data, List<Application> applications, int page) {
////        try {
////            Long chatId = data.getChatId();
////            List<Long> applicationIds = applicationService.getUserApplicationIds(chatId);
////
////            if (applicationIds.isEmpty()) {
////                String text = """
////                    📨 <b>ОТКЛИКНУТНЫЕ ПРОЕКТЫ</b>
////
////                    📭<i> Вы еще не откликались на проекты</i>
////
////                    💡 *Как найти проекты:*
////                    • Используйте поиск проектов
////                    • Изучите требования заказчиков
////                    • Отправляйте качественные отклики
////                    """;
////                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
////                return;
////            }
////
////            // 🔥 ИСПОЛЬЗУЕМ PAGINATION MANAGER
////            paginationManager.renderIdBasedPage(
////                    chatId,
////                    APPLICATIONS_CONTEXT_KEY,
////                    applicationIds,
////                    "APPLICATION",
////                    "init",
////                    APPLICATIONS_PER_PAGE,
////                    this::renderFreelancerApplicationsPage
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка показа страницы откликов: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки откликов", 5);
////        }
////    }
//
////    // 🔥 ФОРМАТИРОВАНИЕ ПРЕВЬЮ ОТКЛИКА
////    private String formatApplicationPreview(Application application, int number) {
////
////        ProjectDto project = projectService.getProjectDtoById(application.getProjectId()).orElseThrow(() -> new RuntimeException("Проект не найден"));
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
////                project.getTitle(),
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
////    // 🔥 ТЕКСТ ПАГИНАЦИИ ДЛЯ ОТКЛИКОВ
////    private String createApplicationsPaginationText(List<Application> applications, int page) {
////        int pageSize = 5;
////        int totalPages = (int) Math.ceil((double) applications.size() / pageSize);
////        int startApplication = (page * pageSize) + 1;
////        int endApplication = Math.min((page + 1) * pageSize, applications.size());
////
////        return """
////        📄 **СТРАНИЦА %d ИЗ %d**
////        """.formatted(
////                page + 1,
////                totalPages
////        );
////    }
//
//
////    // 🔥 ПАГИНАЦИЯ ДЛЯ ОТКЛИКОВ
////    private void handleApplicationsPagination(ProjectData data, String direction) {
////        try {
////            List<Application> applications = userSessionService.getFromContext(data.getChatId(),
////                    "my_applications_list", List.class);
////            Integer currentPage = userSessionService.getFromContext(data.getChatId(),
////                    "my_applications_page", Integer.class);
////
////            if (applications == null || currentPage == null) {
////                showMyApplications(data);
////                return;
////            }
////
////            int totalPages = (int) Math.ceil((double) applications.size() / 5);
////            int newPage = currentPage;
////
////            if ("next".equals(direction) && currentPage < totalPages - 1) {
////                newPage = currentPage + 1;
////            } else if ("prev".equals(direction) && currentPage > 0) {
////                newPage = currentPage - 1;
////            }
////
////            userSessionService.putToContext(data.getChatId(), "my_applications_page", newPage);
////
////            // 🔥 УДАЛЯЕМ СТАРЫЕ СООБЩЕНИЯ И ПОКАЗЫВАЕМ НОВУЮ СТРАНИЦУ
////            deletePreviousMessages(data.getChatId());
////            showApplicationsPage(data, applications, newPage);
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка пагинации откликов: {}", e.getMessage());
////            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
////        }
////    }
//
//    public void showActiveProjects(ProjectData data) {
//        try {
//            // 🔥 РЕАЛЬНАЯ ЛОГИКА - получение активных проектов пользователя
//            List<Project> activeProjects = projectService.getFreelancerProjects(data.getChatId())
//                    .stream()
//                    .filter(p -> p.getStatus() == UserRole.ProjectStatus.IN_PROGRESS)
//                    .collect(Collectors.toList());
//
//            if (activeProjects.isEmpty()) {
//                String text = """
//                    ⚙️ <b>**ВЫПОЛНЯЕМЫЕ ПРОЕКТЫ**</b>
//
//                    📊 <i>Сейчас у вас нет активных проектов</i>
//
//                    💡 *Как получить заказы:*
//                    • Активно откликайтесь на проекты
//                    • Следите за своим рейтингом
//                    • Предлагайте конкурентные условия
//                    """;
//                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
//                return;
//            }
//
//            // Показываем активные проекты
//            showActiveProjectsList(data, activeProjects);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа активных проектов: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки активных проектов", delaySeconds);
//        }
//    }
//
//    /**
//     * Отображает пользователю главный баннер поиска с кнопками фильтров.
//     */
//    private void showSearchForm(Long chatId, Integer messageIdToEdit, SearchRequest currentRequest) {
//
//        // 1. Текст баннера
//        String text = """
//    🔍<b> **ПОИСК ПРОЕКТОВ TCMatch** </b>
//
//    🚀 <i>*Выберите фильтр для начала поиска*</i>
//    """;
//
//        // 2. Клавиатура фильтров
//        // 🔥 ВАЖНО: Клавиатура должна быть построена на основе текущего SearchRequest
//        InlineKeyboardMarkup keyboard = keyboardFactory.createFilterSelectionKeyboard(currentRequest);
//
//        // 3. Отправка/редактирование
//        // Используем BotExecutor (унаследованный от BaseHandler)
//        if (messageIdToEdit != null) {
//            botExecutor.editMessageWithHtml(chatId, messageIdToEdit, text, keyboard);
//        } else {
//            userSessionService.setMainMessageId(chatId, botExecutor.sendHtmlMessageReturnId(chatId, text, keyboard));
//        }
//    }
//
//    public void showProjectSearch(ProjectData data) {
//        try {
//            String filter = data.getFilter() != null ? data.getFilter() : "";
//
//            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ ПЕРЕД НОВЫМ ПОИСКОМ
//            deletePreviousMessages(data.getChatId());
//
//            // 🔥 ЕСЛИ У НАС ЕЩЁ НЕТ СОХРАНЕННОГО MESSAGE_ID - СОХРАНЯЕМ ЕГО
//            if (getMainMessageId(data.getChatId()) == null) {
//                saveMainMessageId(data.getChatId(), data.getMessageId());
//            }
//
//            // 🔥 ВСЕГДА ИСПОЛЬЗУЕМ СОХРАНЕННЫЙ MESSAGE_ID
//            Integer mainMessageId = getMainMessageId(data.getChatId());
//
//            // 🔥 ЕСЛИ ФИЛЬТР ПУСТОЙ - ПОКАЗЫВАЕМ ТОЛЬКО ИНТЕРФЕЙС ПОИСКА
//            if (filter.isEmpty()) {
//                String text = """
//                🔍<b> **ПОИСК ПРОЕКТОВ TCMatch** </b>
//
//                🚀 <i>*Выберите фильтр для начала поиска*</i>
//                """;
//
//                InlineKeyboardMarkup keyboard = keyboardFactory.createSearchControlKeyboard(filter);
//                editMessageWithHtml(data.getChatId(), mainMessageId, text, keyboard);
//                return;
//            }
//
//            ProjectSearchService.SearchState searchState = projectSearchService.getOrCreateSearchState(data.getChatId(), filter);
//            List<Project> searchResults = searchState.projects;
//
//            if (searchResults.isEmpty()) {
//                String text = """
//                    🔍 <b>**ПРОЕКТЫ НЕ НАЙДЕНЫ**</b>
//
//                    💡<i> Попробуйте:
//                    • Изменить фильтры поиска
//                    • Расширить критерии поиска
//                    • Проверить позже</i>
//                    """;
//                editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createSearchControlKeyboard(filter));
//                return;
//            }
//
//            List<Project> pageProjects = projectSearchService.getPageProjects(data.getChatId(), filter);
//
//            log.debug("🔍 DEBUG: pageProjects.size() = {}, currentPage = {}",
//                    pageProjects.size(), searchState.currentPage);
//
//            editMessageWithHtml(data.getChatId(), userSessionService.getMainMessageId(data.getChatId()), "<b>🔍Найдено проектов:</b>: %d".formatted(searchResults.size()), null);
//
//            // 🔥 ОТПРАВЛЯЕМ КАЖДЫЙ ПРОЕКТ ОТДЕЛЬНЫМ СООБЩЕНИЕМ
//            List<Integer> newMessageIds = new ArrayList<>();
//            for (int i = 0; i < pageProjects.size(); i++) {
//                Project project = pageProjects.get(i);
//                String projectText = formatProjectPreview(project, i + 1);
//
//                // 🔥 УПРОЩЕННАЯ КЛАВИАТУРА - ТОЛЬКО "ДЕТАЛИ"
//                InlineKeyboardMarkup projectKeyboard = keyboardFactory.createProjectPreviewKeyboard(project.getId());
//
//                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), projectText, projectKeyboard);
//                if (newMessageId != null) {
//                    newMessageIds.add(newMessageId);
//                }
//            }
//
//
//
//            // 🔥 ОТПРАВЛЯЕМ ПАГИНАЦИЮ КАК ОТДЕЛЬНОЕ СООБЩЕНИЕ ПОСЛЕ ПРОЕКТОВ
//            String paginationText = createPaginationText(data.getChatId(), searchState);
//            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboard(filter, data.getChatId());
//
//            Integer paginationMessageId = sendHtmlMessageReturnId(data.getChatId(), paginationText, paginationKeyboard);
//            if (paginationMessageId != null) {
//                newMessageIds.add(paginationMessageId);
//            }
//
//            // 🔥 СОХРАНЯЕМ ID НОВЫХ СООБЩЕНИЙ
//            saveProjectMessageIds(data.getChatId(), newMessageIds);
//
//            // 🔥 ОТПРАВЛЯЕМ СООБЩЕНИЕ С ПАГИНАЦИЕЙ И УПРАВЛЕНИЕМ
//            String controlText = """
//            📊 <b>**РЕЗУЛЬТАТЫ ПОИСКА**</b>
//            """.formatted(
//                    searchResults.size(),
//                    searchState.currentPage + 1
//            );
//
//            InlineKeyboardMarkup controlKeyboard = keyboardFactory.createSearchControlKeyboard(filter);
//            editMessageWithHtml(data.getChatId(), data.getMessageId(), controlText, controlKeyboard);
//        } catch (Exception e) {
//            log.error("❌ Ошибка поиска проектов: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка поиска проектов", 5);
//        }
//    }
//
//    private  String createPaginationText(Long chatId, ProjectSearchService.SearchState state) {
//        int totalPages = (int) Math.ceil((double) state.projects.size() / state.pageSize);
//        int startProject = (state.currentPage * state.pageSize) + 1;
//        int endProject = Math.min((state.currentPage + 1) * state.pageSize, state.projects.size());
//
//        return """
//        📄 **СТРАНИЦА %d ИЗ %d**
//        """.formatted(
//                state.currentPage + 1,
//                totalPages
//        );
//    }
//
//    // 🔥 СПЕЦИАЛЬНЫЙ ФОРМАТ ДЛЯ ОТКЛИКА
//    private String formatProjectDetailsForApplication(ProjectDto projectdto) {
//        return """
//                📝 **ОТКЛИК НА ПРОЕКТ**
//
//                💼 *Название проекта:* %s
//                💰 *Бюджет:* %.0f руб
//                ⏱️ *Срок выполнения:* %d дней
//                📅 *Дедлайн:* %s
//
//                📊 *Статистика проекта:*
//                👀 Просмотров: %d
//                📨 Откликов: %d
//
//                👔 *Заказчик:* @%s
//                ⭐ *Рейтинг заказчика:* %.1f/5.0
//
//                📝 *Описание проекта:*
//                %s
//
//                🛠️ *Требуемые навыки:*
//                %s
//
//                💡 *Для отклика нажмите кнопку ниже*
//                """.formatted(
//                projectdto.getTitle(),
//                projectdto.getBudget(),
//                projectdto.getEstimatedDays(),
//                projectdto.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
//                projectdto.getViewsCount(),
//                projectdto.getApplicationsCount(),
//                projectdto.getCustomerUserName() != null ? projectdto.getCustomerUserName() : "скрыт",
//                projectdto.getCustomerRating(),
//                projectdto.getDescription(),
//                projectdto.getRequiredSkills() != null ? projectdto.getRequiredSkills() : "не указаны"
//        );
//    }
//
//    public void handleProjectFilterAction(ProjectData data, String parameter) {
//        Long chatId = data.getChatId();
//        Integer messageId = data.getMessageId();
//
//        // Получаем текущий DTO
//        SearchRequest currentRequest = userSessionService.getFromContext(chatId, SEARCH_STATE_KEY, SearchRequest.class);
//        if (currentRequest == null) {
//            currentRequest = SearchRequest.empty();
//        }
//
//        // --- 1. НАЧАЛО ИЛИ ПЕРЕРИСОВКА ФОРМЫ ---
//        if (parameter == null || parameter.isEmpty() ||"start".equals(parameter) || "clear".equals(parameter)) {
//
//            if ("clear".equals(parameter)) {
//                currentRequest = SearchRequest.empty();
//                userSessionService.putToContext(chatId, SEARCH_STATE_KEY, currentRequest);
//            }
//
//            // Показываем форму фильтрации (Режим 1)
//            showSearchForm(chatId, messageId, currentRequest);
//            return;
//        }
//
//        // --- 2. ПРИМЕНЕНИЕ ФИЛЬТРОВ И ПЕРЕХОД К ПАГИНАЦИИ ---
//        if ("apply".equals(parameter)) {
//            // Логика перехода к Режиму 2
//            handleProjectSearchInitialization(chatId, currentRequest, messageId);
//            return;
//        }
//
//        if (parameter.startsWith("budget:")) {
//            // 1. ИЗВЛЕКАЕМ ЗНАЧЕНИЕ БЮДЖЕТА
//            // Наша строка: "budget:50000" или "budget:clear"
//            String budgetValue = parameter.substring("budget:".length());
//            handleBudgetFilter(chatId, messageId, budgetValue);
//            return;
//        }
//    }
//
//    private void handleBudgetFilter(Long chatId, Integer messageIdToEdit, String value) {
//
//        // 1. Парсинг значения
//        int newMinBudget = 0;
//        try {
//            if ("clear".equals(value)) {
//                newMinBudget = 0; // Сброс
//            } else {
//                newMinBudget = Integer.parseInt(value);
//            }
//        } catch (NumberFormatException e) {
//            log.error("❌ Некорректное значение бюджета: {}", value);
//            return;
//        }
//
//        // 2. Обновление DTO в сессии
//        SearchRequest currentRequest = userSessionService.getFromContext(chatId, SEARCH_STATE_KEY, SearchRequest.class);
//        if (currentRequest == null) {
//            currentRequest = SearchRequest.empty();
//        }
//
//        currentRequest.setMinBudget(newMinBudget > 0 ? newMinBudget : null);
//        userSessionService.putToContext(chatId, SEARCH_STATE_KEY, currentRequest);
//
//        // 3. Перерисовка формы (для обновления галочек)
//        showSearchForm(chatId, messageIdToEdit, currentRequest);
//    }
//
//    public void handleProjectSearchInitialization(Long chatId, SearchRequest searchRequest, Integer mainMessageId) {
//
//        // 1. Загрузка ВСЕГО списка
//        List<Long> searchResultIds = projectService.searchActiveProjectIds(searchRequest);
//
//        // 2. БАННЕР: Редактирование главного сообщения (вместо удаления)
//        if (searchResultIds.isEmpty()) {
//            String notFoundText = """
//            🔍 <b>**ПРОЕКТЫ НЕ НАЙДЕНЫ**</b>
//
//            💡<i> По вашему запросу ничего не нашлось.</i>
//            """;
//            // Клавиатура: Только кнопка "Изменить фильтр"
//            InlineKeyboardMarkup keyboard = keyboardFactory.createOneButtonKeyboard("✏️ Изменить фильтр", "projects:filter:start");
//
//            // 🔥 РЕДАКТИРУЕМ ГЛАВНОЕ СООБЩЕНИЕ
//            botExecutor.editMessageWithHtml(chatId, mainMessageId, notFoundText, keyboard);
//        }
//
//        // 3. Сохранение DTO и запуск пагинации
//        userSessionService.putToContext(chatId, SEARCH_STATE_KEY, searchRequest);
//
//        // 4. Делегирование PaginationManager:
//        paginationManager.renderIdBasedPage(
//                chatId,
//                PaginationContextKeys.PROJECT_SEARCH_CONTEXT_KEY,
//                searchResultIds,
//                "PROJECT",
//                "init",
//                PROJECTS_PER_PAGE,
//                this::renderSearchPage
//        );
//    }
//
//
//
//    public void handleSearchFilter(ProjectData data, String filter) {
//        try {
//
//            // 🔥 ОЧИЩАЕМ ТЕКУЩИЙ ПОИСК ДЛЯ ПРИМЕНЕНИЯ НОВОГО ФИЛЬТРА
//            projectSearchService.clearSearchState(data.getChatId());
//
//            // 🔥 СОЗДАЕМ НОВЫЙ ProjectData С ФИЛЬТРОМ
//            ProjectData filteredData = new ProjectData(
//                    data.getChatId(),
//                    data.getMessageId(),
//                    data.getUserName(),
//                    filter,
//                    "search",
//                    null
//            );
//
//            // 🔥 ЗАПУСКАЕМ ПОИСК С НОВЫМ ФИЛЬТРОМ
//            showProjectSearch(filteredData);
//        } catch (Exception e) {
//            log.error("❌ Ошибка применения фильтра: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка применения фильтра", 5);
//        }
//    }
//
//    private void handlePagination(ProjectData data, String parameter) {
//        try {
//            // 🔥 НОВЫЙ ФОРМАТ ПАРАМЕТРА: "next:favorites:PROJECT" или "prev:search:PROJECT"
//            String[] parts = parameter.split(":");
//            if (parts.length < 3) {
//                log.error("❌ Неверный формат параметра пагинации: {}", parameter);
//                return;
//            }
//
//            String direction = parts[0];   // "next" или "prev"
//            String contextKey = parts[1];  // "favorites", "search", "my_applications"
//            String entityType = parts[2];  // "PROJECT" или "APPLICATION"
//
//            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = getContextRenderer(contextKey);
//
//            int pageSize = getPageSizeForContext(contextKey);
//
//            if (renderer == null) {
//                log.error("❌ Renderer not found for context: {}", contextKey);
//                return;
//            }
//
//            // Вызываем универсальный менеджер
//            paginationManager.renderIdBasedPage(
//                    data.getChatId(),
//                    contextKey,
//                    null, // Список уже в сессии
//                    entityType,
//                    direction,
//                    PROJECTS_PER_PAGE,
//                    renderer
//            );
////
////            // 🔥 ДЕЛЕГИРОВАНИЕ: Для перехода нам не нужно передавать полный список (он уже в сессии)
////            if (FAVORITES_CONTEXT_KEY.equals(contextKey)) {
////                paginationManager.renderPage(
////                        data.getChatId(),
////                        contextKey,
////                        null, // Список уже в сессии
////                        direction, // "next" или "prev"
////                        PROJECTS_PER_PAGE,
////                        this::renderFavoritesPage);
////            }
////
////
////            // 🔥 ПАГИНАЦИЯ ДЛЯ ОТКЛИКОВ
////            if (parameter.startsWith("applications:")) {
////                handleApplicationsPagination(data, parameter.replace("applications:", ""));
////                return;
////            }
////
////            // 🔥 ПАГИНАЦИЯ ДЛЯ "МОИХ ПРОЕКТОВ"
////            if (parameter.startsWith("my_list:")) {
////                handleMyProjectsPagination(data, parameter);
////                return;
////            }
////
////            if (parameter.startsWith("favorites")) {
////                handleFavoritesPagination(data, parameter.replace("applications:", ""));
////                return;
////            }
////
////            if ("next".equals(direction)) {
////                projectSearchService.nextPage(data.getChatId());
////            } else if ("prev".equals(direction)) {
////                projectSearchService.prevPage(data.getChatId());
////            }
////
////            ProjectData searchData = new ProjectData(data.getChatId(), data.getMessageId(), data.getUserName(), filter, "search", null);
////            showProjectSearch(searchData);
//        } catch (Exception e) {
//            log.error("❌ Ошибка пагинации: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
//        }
//    }
//
//    public BiFunction<List<Long>, PaginationContext, List<Integer>> getContextRenderer(String contextKey) {
//        switch (contextKey) {
//            case PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY:
//                return this::renderFavoritesPage;
//            case PaginationContextKeys.PROJECT_SEARCH_CONTEXT_KEY:
//                return this::renderSearchPage;
//            default:
//                return null;
//        }
//    }
//
//    private List<Project> getFavoriteProjects(Long chatId) {
//        List<Project> projects = new ArrayList<>();
//        for (Long projectId : userService.getFavoriteProjectIds(chatId)) {
//            Project project = projectService.getProjectById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));
//            projects.add(project);
//        }
//        return projects;
//    }
//
//    private void showProjectWithPagination(ProjectData data, List<Project> projects, int currentIndex, String context) {
//        if (projects.isEmpty() || currentIndex >= projects.size()) return;
//
//        Project project = projects.get(currentIndex);
//        String projectText = formatProjectPreview(project, currentIndex + 1);
//        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectWithPaginationKeyboard(
//                project.getId(), currentIndex, projects.size(), context
//        );
//
//        editMessageWithHtml(data.getChatId(), data.getMessageId(), projectText, keyboard);
//    }
//
////    private void showApplicationsList(ProjectData data, List<Application> applications) {
////        StringBuilder text = new StringBuilder("📨 **ВАШИ ОТКЛИКИ**\n\n");
////
////        for (int i = 0; i < Math.min(applications.size(), 10); i++) {
////            Application app = applications.get(i);
////            Project project = app.getProject();
////
////            text.append("""
////                    %d. 💼 *%s*
////                       💰 Бюджет: %.0f руб
////                       ⏱️ Срок: %d дней
////                       📊 Статус: %s
////                       📅 Отправлен: %s
////
////                    """.formatted(
////                    i + 1,
////                    project.getTitle(),
////                    project.getBudget(),
////                    project.getEstimatedDays(),
////                    getApplicationStatusDisplay(app.getStatus()),
////                    app.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
////            ));
////        }
////
////        editMessageWithHtml(data.getChatId(), data.getMessageId(), text.toString(), keyboardFactory.createBackButton());
////    }
//
//    private String formatProjectDetails(ProjectDto project) {
//        return """
//            <b>💼 **ДЕТАЛИ ПРОЕКТА**</b>
//
//            <blockquote><b>🎯 *Название:*</b> %s
//            <b>💰 *Бюджет:*</b> %.0f руб
//            <b>⏱️ *Срок:*</b> %d дней
//            <b>📅 *Дедлайн:*</b> %s
//            <b>👀 *Просмотров:*</b> %d
//            <b>📨 *Откликов:*</b> %d
//
//            <b>📝 *Описание:*</b>
//            <i>%s</i>
//
//            <b>🛠️ *Требуемые навыки:*</b>
//            <u>%s</u></blockquote>
//
//            <b>👔 *Заказчик:*</b> @%s
//            <b>📊 *Рейтинг заказчика:*</b> ⭐ %.1f/5.0
//            """.formatted(
//                project.getTitle(),
//                project.getBudget(),
//                project.getEstimatedDays(),
//                project.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
//                project.getViewsCount(),
//                project.getApplicationsCount(),
//                project.getDescription(),
//                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны",
//                project.getCustomerUserName() != null ? project.getCustomerUserName() : "скрыт",
//                project.getCustomerRating()
//        );
//    }
//
//    private String formatProjectPreview(Project project, int number) {
//        return """
//            🎯 <b>**Проект #%d**</b>
//
//            <blockquote><b>💼 *%s*</b>
//            <b>💰 Бюджет:</b> *%.0f руб*
//            <b>⏱️ Срок:</b> *%d дней*
//            <b>👀 Просмотров:</b> *%d*
//            <b>📨 Откликов:</b> *%d*
//
//            📝 <i>%s</i></blockquote>
//            """.formatted(
//                number,
//                project.getTitle(),
//                project.getBudget(),
//                project.getEstimatedDays(),
//                project.getViewsCount(),
//                project.getApplicationsCount(),
//                project.getDescription().length() > 100 ?
//                        project.getDescription().substring(0, 100) + "..." :
//                        project.getDescription()
//        );
//    }
//
//    private void showActiveProjectsList(ProjectData data, List<Project> activeProjects) {
//        String text = "<b>🚧 Раздел 'Выполняемые' в разработке...</b>";
//        editMessageWithHtml(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
//    }
//
////    public List<Integer> renderFreelancerApplicationsPage(List<Long> pageApplicationIds, PaginationContext<Application> context) {
////        Long chatId = context.chatId();
////        List<Integer> messageIds = new ArrayList<>();
////
////        // Получаем отклики по ID
////        List<Application> pageApplications = applicationService.findAllApplicationsByIds(pageApplicationIds);
////
////        // Заголовок
////        String headerText = String.format("""
////                📨 <b>МОИ ОТКЛИКИ</b>
////
////                <i>Найдено %d откликов. Страница %d из %d</i>
////                """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());
////        Integer headerId = sendHtmlMessageReturnId(chatId, headerText, null);
////        if (headerId != null) messageIds.add(headerId);
////
////        for (int i = 0; i < pageApplications.size(); i++) {
////            Application application = pageApplications.get(i);
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
////            InlineKeyboardMarkup keyboard = keyboardFactory.createUniversalPaginationKeyboard(
////                    context.currentPage(), context.entityIds().size(), context.pageSize(), APPLICATIONS_CONTEXT_KEY);
////            Integer navId = sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", keyboard);
////            if (navId != null) messageIds.add(navId);
////        }
////        return messageIds;
////    }
//
//    // 🔥 Функция рендеринга: принимает данные и возвращает список ID отправленных сообщений
//    public List<Integer> renderFavoritesPage(List<Long> pageProjectIds, PaginationContext<Project> context) {
//        Long chatId = context.chatId();
//        List<Integer> messageIds = new ArrayList<>();
//
//        // Получаем проекты по ID
//        List<Project> pageProjects = projectService.findAllProjectsByIds(pageProjectIds);
//
//// Заголовок
//        String headerText = String.format("""
//                ⭐ <b>ИЗБРАННЫЕ ПРОЕКТЫ</b>
//
//                <i>Найдено %d проектов. Страница %d из %d</i>
//                """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());
//
//
//        editMessageWithHtml(chatId, userSessionService.getMainMessageId(chatId), headerText, null);
//
//        // 2. Отправка Карточек
//        for (int i = 0; i < pageProjects.size(); i++) {
//            Project project = pageProjects.get(i);
//            String projectCardText = formatProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);
//
//            InlineKeyboardMarkup keyboard = keyboardFactory.createProjectPreviewKeyboard(project.getId());
//            Integer cardId = sendHtmlMessageReturnId(chatId, projectCardText, keyboard);
//            if (cardId != null) messageIds.add(cardId);
//        }
//
//        // Пагинация
//
//        InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboardForContext(context);
//        Integer navId = sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
//        if (navId != null) messageIds.add(navId);
//
//        return messageIds;
//    }
//
//    /**
//     * Функция рендеринга, которая отправляет сообщения для текущей страницы.
//     * (BiFunction<List<Project>, PaginationContext<Project>, List<Integer>>)
//     */
//    /**
//     * Функция рендеринга, которая отправляет сообщения для текущей страницы.
//     * (Вызывается PaginationManager)
//     */
//    public List<Integer> renderSearchPage(List<Long> pageProjectIds, PaginationContext<Project> context) {
//        Long chatId = context.chatId();
//        List<Integer> messageIds = new ArrayList<>();
//
//        // Получаем проекты по ID
//        List<Project> pageProjects = projectService.findAllProjectsByIds(pageProjectIds);
//
//        //Карточки Проектов
//        for (int i = 0; i < pageProjects.size(); i++) {
//            Project project = pageProjects.get(i);
//            // Расчет номера проекта для форматирования
//            String projectText = formatProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);
//
//            // Клавиатура: "Детали" / "Откликнуться"
//            InlineKeyboardMarkup projectKeyboard = keyboardFactory.createProjectPreviewKeyboard(project.getId());
//
//            Integer newMessageId = sendHtmlMessageReturnId(chatId, projectText, projectKeyboard);
//            if (newMessageId != null) {
//                messageIds.add(newMessageId);
//            }
//        }
//
//        // Пагинация
//
//        InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboardForContext(context);
//        Integer navId = sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
//        if (navId != null) messageIds.add(navId);
//
//
//        editMessageWithHtml(chatId, userSessionService.getMainMessageId(chatId), "<b>🔍Найденные проекты:</b>:".formatted(context.getTotalPages()), null);
//
//        return messageIds;
//    }
//
//    // 🔥 РЕНДЕРЕР ДЛЯ СТРАНИЦЫ ПРОЕКТОВ ЗАКАЗЧИКА
//    public List<Integer> renderCustomerProjectsPage(List<Long> pageProjectIds, PaginationContext context) {
//        Long chatId = context.chatId();
//        List<Integer> messageIds = new ArrayList<>();
//
//        // Получаем проекты по ID
//        List<Project> pageProjects = projectService.findAllProjectsByIds(pageProjectIds);
//
//        // Отправляем карточки проектов
//        for (int i = 0; i < pageProjects.size(); i++) {
//            Project project = pageProjects.get(i);
//            String projectText = formatCustomerProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);
//
//            // Клавиатура для карточки проекта
//            InlineKeyboardMarkup projectKeyboard = keyboardFactory.createProjectDetailsKeyboard(project.getId(), false);
//
//            Integer cardId = sendHtmlMessageReturnId(chatId, projectText, projectKeyboard);
//            if (cardId != null) messageIds.add(cardId);
//        }
//
//        // Пагинация (если нужно)
//        if (context.getTotalPages() > 1) {
//            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboardForContext(context);
//            Integer navId = sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
//            if (navId != null) messageIds.add(navId);
//        }
//
//        return messageIds;
//    }
//
//    // 🔥 ФОРМАТИРОВАНИЕ КАРТОЧКИ ПРОЕКТА ДЛЯ ЗАКАЗЧИКА
//    private String formatCustomerProjectPreview(Project project, int number) {
//        return """
//        🎯 <b>**Проект #%d**</b>
//
//        <blockquote><b>💼 %s</b>
//        <b>💰 Бюджет:</b> %.0f руб
//        <b>⏱️ Срок:</b> %d дней
//        <b>📊 Статус:</b> %s
//        <b>👀 Просмотров:</b> %d
//        <b>📨 Откликов:</b> %d</blockquote>
//        """.formatted(
//                number,
//                project.getTitle(),
//                project.getBudget(),
//                project.getEstimatedDays(),
//                getProjectStatusDisplay(project.getStatus()),
//                project.getViewsCount(),
//                project.getApplicationsCount()
//        );
//    }
//
//    private String getFilterDisplay(String filter) {
//        return switch (filter) {
//            case "all" -> "Все проекты";
//            case "open" -> "Открытые";
//            case "in_progress" -> "В работе";
//            case "completed" -> "Завершенные";
//            default -> "Проекты";
//        };
//    }
//
//    private void startProjectCreation(ProjectData data) {
//        try {
//            Long chatId = data.getChatId();
//
//            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ
//            deletePreviousMessages(chatId);
//
//            // 🔥 ЗАПУСКАЕМ ПРОЦЕСС СОЗДАНИЯ
//            projectCreationService.startProjectCreation(chatId);
//            showCurrentProjectCreationStep(data);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка начала создания проекта: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка начала создания проекта", 5);
//        }
//    }
//
//    private void showCurrentProjectCreationStep(ProjectData data) {
//        ProjectCreationState state = projectCreationService.getCurrentState(data.getChatId());
//
//        if (state == null) return;
//
//        String text = "";
//        InlineKeyboardMarkup keyboard = null;
//
//        if (state.isEditing()) {
//            // 🔥 РЕЖИМ РЕДАКТИРОВАНИЯ
//            text = getProjectEditStepInfo(state);
//            keyboard = keyboardFactory.createProjectEditKeyboard(state.getCurrentStep().name().toLowerCase());
//        } else if (state.getCurrentStep() == ProjectCreationState.ProjectCreationStep.CONFIRMATION) {
//            // 🔥 ЭКРАН ПОДТВЕРЖДЕНИЯ
//            text = formatProjectConfirmation(state);
//            keyboard = keyboardFactory.createProjectConfirmationKeyboard();
//        } else {
//            // 🔥 ПРОЦЕСС ЗАПОЛНЕНИЯ
//            text = getProjectStepText(state);
//            keyboard = keyboardFactory.createProjectCreationKeyboard();
//        }
//
//        Integer mainMessageId = getMainMessageId(data.getChatId());
//        if (mainMessageId != null) {
//            editMessageWithHtml(data.getChatId(), mainMessageId, text, keyboard);
//        }
//    }
//
//    private void handleProjectCreationCallback(ProjectData data, String action, String parameter) {
//        switch (action) {
//            case "edit_field":
//                editProjectField(data, parameter);
//                break;
//            case "edit_cancel":
//                cancelProjectEditing(data);
//                break;
//            case "confirm":
//                confirmProjectCreation(data);
//                break;
//            case "cancel":
//                cancelProjectCreation(data);
//                break;
//            default:
//                log.warn("❌ Unknown project creation action: {}", action);
//        }
//    }
//
//    // 🔥 РЕДАКТИРОВАНИЕ ПОЛЯ ПРОЕКТА
//    private void editProjectField(ProjectData data, String field) {
//        try {
//            ProjectCreationState state = projectCreationService.getCurrentState(data.getChatId());
//            if (state == null) return;
//
//            // 🔥 ПЕРЕХОДИМ В РЕЖИМ РЕДАКТИРОВАНИЯ КОНКРЕТНОГО ПОЛЯ
//            state.moveToEditField(field);
//            projectCreationService.updateCurrentState(data.getChatId(), state);
//
//            showCurrentProjectCreationStep(data);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка редактирования поля проекта: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка редактирования проекта", 5);
//        }
//    }
//
//    // 🔥 ОТМЕНА РЕДАКТИРОВАНИЯ
//    private void cancelProjectEditing(ProjectData data) {
//        try {
//            ProjectCreationState state = projectCreationService.getCurrentState(data.getChatId());
//            if (state == null) return;
//
//            // 🔥 ВОЗВРАЩАЕМСЯ В РЕЖИМ ПОДТВЕРЖДЕНИЯ
//            state.finishEditing();
//            projectCreationService.updateCurrentState(data.getChatId(), state);
//
//            showCurrentProjectCreationStep(data);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка отмены редактирования: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка отмены редактирования", 5);
//        }
//    }
//
//    // 🔥 ПОДТВЕРЖДЕНИЕ СОЗДАНИЯ ПРОЕКТА
//    private void confirmProjectCreation(ProjectData data) {
//        try {
//            ProjectCreationState state = projectCreationService.getCurrentState(data.getChatId());
//            if (state == null) return;
//
//            if (!state.isCompleted()) {
//                sendTemporaryErrorMessage(data.getChatId(), "❌ Заполните все поля проекта", 5);
//                return;
//            }
//
//            // 🔥 СОЗДАЕМ ПРОЕКТ В БАЗЕ ДАННЫХ
//            Project project = projectService.createProject(
//                    data.getChatId(),
//                    state.getTitle(),
//                    state.getDescription(),
//                    state.getBudget(),
//                    null, // deadline будет вычислен на основе estimatedDays
//                    state.getRequiredSkills(),
//                    state.getEstimatedDays()
//            );
//
//            projectCreationService.completeCreation(data.getChatId());
//
//            String successText = """
//            <b>✅ ПРОЕКТ СОЗДАН!</b>
//
//            <blockquote><b>🎯 Название:</b> %s
//            <b>💰 Бюджет:</b> <code>%.0f руб</code>
//            <b>⏱️ Срок:</b> <code>%d дней</code>
//            <b>🛠️ Навыки:</b> %s</blockquote>
//
//            <b>🚀 Проект теперь доступен исполнителям</b>
//            <i>💡 Вы можете управлять проектом в разделе "Мои проекты"</i>
//            """.formatted(
//                    project.getTitle(),
//                    project.getBudget(),
//                    project.getEstimatedDays(),
//                    project.getRequiredSkills()
//            );
//
//            Integer mainMessageId = getMainMessageId(data.getChatId());
//            editMessageWithHtml(data.getChatId(), mainMessageId, successText,
//                    keyboardFactory.createToMainMenuKeyboard());
//
//            log.info("✅ Пользователь {} создал проект {}", data.getChatId(), project.getId());
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка создания проекта: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка создания проекта: " + e.getMessage(), 5);
//        }
//    }
//
//    // 🔥 ОТМЕНА СОЗДАНИЯ ПРОЕКТА
//    private void cancelProjectCreation(ProjectData data) {
//        projectCreationService.cancelCreation(data.getChatId());
//
//        String text = """
//        ❌ <b>СОЗДАНИЕ ПРОЕКТА ОТМЕНЕНО</b>
//
//        <i>💡 Вы можете создать проект позже через меню "Мои проекты"</i>
//        """;
//
//        Integer mainMessageId = getMainMessageId(data.getChatId());
//        editMessageWithHtml(data.getChatId(), mainMessageId, text,
//                keyboardFactory.createToMainMenuKeyboard());
//
//        log.info("❌ Пользователь {} отменил создание проекта", data.getChatId());
//    }
//
//    private String getProjectEditStepInfo(ProjectCreationState state) {
//        String currentValue = "";
//        String instruction = "";
//        switch (state.getCurrentStep()) {
//            case TITLE:
//                currentValue = state.getTitle() != null ?
//                        state.getTitle() : "<i>не указано</i>";
//                instruction = "<b>✏️ Введите новое название проекта:</b>";
//                break;
//            case DESCRIPTION:
//                currentValue = state.getDescription() != null ?
//                        state.getDescription() :
//                        "<i>не указано</i>";
//                instruction = "<b>📝 Введите новое описание проекта:</b>";
//                break;
//            case BUDGET:
//                currentValue = state.getBudget() != null ?
//                        "<code>" + state.getBudget() + " руб</code>" :
//                        "<i>не указан</i>";
//                instruction = "<b>💰 Введите новый бюджет в рублях:</b>";
//                break;
//            case DEADLINE:
//                currentValue = state.getEstimatedDays() != null ?
//                        "<code>" + state.getEstimatedDays() + " дней</code>" :
//                        "<i>не указан</i>";
//                instruction = "<b>⏱️ Введите новые сроки в днях:</b>";
//                break;
//            case SKILLS:
//                currentValue = state.getRequiredSkills() != null ?
//                        state.getRequiredSkills() :
//                        "<i>не указаны</i>";
//                instruction = "<b>🛠️ Введите новые требуемые навыки:</b>";
//                break;
//            default:
//                return "";
//        }
//
//        return """
//        <b>✏️ РЕДАКТИРОВАНИЕ ПРОЕКТА</b>
//
//        <b>📊 Текущее значение:</b>
//        %s
//
//        %s
//
//        <i>💡 После ввода вы вернетесь к подтверждению</i>
//        """.formatted(currentValue, instruction);
//    }
//
//    // 🔥 ТЕКСТ ДЛЯ ОБЫЧНОГО ПРОЦЕССА
//    private String getProjectStepText(ProjectCreationState state) {
//        switch (state.getCurrentStep()) {
//            case TITLE:
//                return """
//                <b>📝 ШАГ 1: НАЗВАНИЕ ПРОЕКТА</b>
//
//                <b>✏️ Что нужно сделать:</b>
//                • Придумайте краткое и понятное название
//                • Отразите суть проекта в названии
//                • Максимум 100 символов
//
//                <b>👇 Введите название проекта в следующем сообщении</b>
//                """;
//
//            case DESCRIPTION:
//                String currentTitle = state.getTitle() != null ?
//                        state.getTitle() : "<i>не указано</i>";
//
//                return """
//                <b>📋 ШАГ 2: ОПИСАНИЕ ПРОЕКТА</b>
//
//                <b>🎯 Название проекта:</b> %s
//
//                <b>📝 Что нужно сделать:</b>
//                • Подробно опишите задачу
//                • Укажите требования и ожидания
//                • Опишите желаемый результат
//                • Минимум 20 символов, максимум 3200
//
//                <b>👇 Введите описание проекта в следующем сообщении</b>
//                """.formatted(currentTitle);
//
//            case BUDGET:
//                String currentDescription = state.getDescription() != null ?
//                        (state.getDescription().length() > 100 ?
//                                state.getDescription().substring(0, 100) + "..." :
//                                state.getDescription()) :
//                        "<i>не указано</i>";
//
//                return """
//                <b>💰 ШАГ 3: БЮДЖЕТ ПРОЕКТА</b>
//
//                <b>🎯 Название проекта:</b> %s
//                <b>📝 Описание:</b> %s
//
//                <b>💸 Что нужно сделать:</b>
//                • Укажите бюджет в рублях
//                • Минимальный бюджет: 1000 руб
//                • Максимальный бюджет: 1 000 000 руб
//
//                <b>👇 Введите бюджет в следующем сообщении</b>
//                """.formatted(
//                        state.getTitle(),
//                        currentDescription
//                );
//
//            case DEADLINE:
//                return """
//                <b>⏱️ ШАГ 4: СРОК ВЫПОЛНЕНИЯ</b>
//
//                <b>🎯 Название проекта:</b> %s
//                <b>💰 Бюджет:</b> <code>%.0f руб</code>
//
//                <b>📅 Что нужно сделать:</b>
//                • Укажите срок выполнения в днях
//                • Минимум: 1 день
//                • Максимум: 365 дней
//
//                <b>👇 Введите срок выполнения в следующем сообщении</b>
//                """.formatted(
//                        state.getTitle(),
//                        state.getBudget()
//                );
//
//            case SKILLS:
//                return """
//                <b>🛠️ ШАГ 5: ТРЕБУЕМЫЕ НАВЫКИ</b>
//
//                <b>🎯 Название проекта:</b> %s
//                <b>💰 Бюджет:</b> <code>%.0f руб</code>
//                <b>⏱️ Срок:</b> <code>%d дней</code>
//
//                <b>🔧 Что нужно сделать:</b>
//                • Перечислите требуемые навыки
//                • Укажите технологии, инструменты
//                • Опишите опыт, который нужен
//                • Можно перечислить через запятую
//
//                <b>👇 Введите требуемые навыки в следующем сообщении</b>
//                """.formatted(
//                        state.getTitle(),
//                        state.getBudget(),
//                        state.getEstimatedDays()
//                );
//
//            default:
//                return "";
//        }
//    }
//
//    // 🔥 ФОРМАТИРОВАНИЕ ПОДТВЕРЖДЕНИЯ
//    private String formatProjectConfirmation(ProjectCreationState state) {
//        return """
//        <b>✅ ПОДТВЕРЖДЕНИЕ СОЗДАНИЯ ПРОЕКТА</b>
//
//        <blockquote><b>🎯 Название:</b> %s
//
//        <b>📝 Описание:</b>
//        <i>%s</i>
//
//        <b>💰 Бюджет:</b> <code>%.0f руб</code>
//        <b>⏱️ Срок:</b> <code>%d дней</code>
//
//        <b>🛠️ Требуемые навыки:</b>
//        <u>%s</u></blockquote>
//
//        <b>💡 Проверьте информацию перед созданием</b>
//        <b>🚀 После создания проект станет доступен исполнителям</b>
//        """.formatted(
//                state.getTitle(),
//                state.getDescription(),
//                state.getBudget(),
//                state.getEstimatedDays(),
//                state.getRequiredSkills()
//        );
//    }
//
//    // 🔥 ОБРАБОТКА ТЕКСТОВЫХ СООБЩЕНИЙ ДЛЯ СОЗДАНИЯ ПРОЕКТА
//    public void handleProjectCreationTextMessage(Long chatId, String text, Integer messageId) {
//        if (!projectCreationService.isCreatingProject(chatId)) {
//            deleteMessage(chatId, messageId);
//            return;
//        }
//
//        ProjectCreationState state = projectCreationService.getCurrentState(chatId);
//        if (state == null) {
//            deleteMessage(chatId, messageId);
//            return;
//        }
//
//        // Сообщение, которое могло остаться после предыдущей ошибки
//        Integer oldMessageIdToDelete = state.getMessageIdToDelete();
//
//        try {
//            // 1. ВАЛИДАЦИЯ и СОХРАНЕНИЕ ДАННЫХ
//            projectCreationService.processInputAndValidate(state, text);
//
//            // 2. УСПЕХ: Ввод принят
//            if (oldMessageIdToDelete != null) {
//                deleteMessage(chatId, oldMessageIdToDelete);
//            }
//
//            // 🔥 Удаление текущего успешного сообщения
//            deleteMessage(chatId, messageId);
//
//            // Очистка
//            state.setMessageIdToDelete(null);
//
//            // Переход: обновляем состояние и переходим к следующему шагу
//            if (state.isEditing()) {
//                state.finishEditing();
//            } else {
//                state.moveToNextStep();
//            }
//
//            projectCreationService.updateCurrentState(chatId, state);
//
//            ProjectData data = new ProjectData(chatId, null, "");
//            showCurrentProjectCreationStep(data);
//
//        } catch (NumberFormatException e) {
//            // Ошибка валидации чисел (БЮДЖЕТ/СРОКИ)
//            if (oldMessageIdToDelete != null) {
//                deleteMessage(chatId, oldMessageIdToDelete);
//                state.setMessageIdToDelete(null);
//                projectCreationService.updateCurrentState(chatId, state);
//            }
//
//            String errorMsg = "❌ Пожалуйста, введите корректное число";
//            deleteMessage(chatId, messageId);
//            sendTemporaryErrorMessage(chatId, errorMsg, 5);
//
//        } catch (Exception e) {
//            // Общая ошибка валидации
//            if (oldMessageIdToDelete != null) {
//                deleteMessage(chatId, oldMessageIdToDelete);
//                state.setMessageIdToDelete(null);
//                projectCreationService.updateCurrentState(chatId, state);
//            }
//
//            deleteMessage(chatId, messageId);
//            sendTemporaryErrorMessage(chatId, "❌ Ошибка: " + e.getMessage(), 5);
//        }
//    }
//
//    private String getProjectStatusIcon(UserRole.ProjectStatus status) {
//        return switch (status) {
//            case OPEN -> "🔓 ";
//            case IN_PROGRESS -> "⚙️ ";
//            case COMPLETED -> "✅ ";
//            case CANCELLED -> "❌ ";
//            default -> "📁 ";
//        };
//    }
//
//    private String getProjectStatusDisplay(UserRole.ProjectStatus status) {
//        return switch (status) {
//            case OPEN -> "Открыт";
//            case IN_PROGRESS -> "В работе";
//            case COMPLETED -> "Завершен";
//            case CANCELLED -> "Отменен";
//            default -> "Неизвестно";
//        };
//    }
//
//    private String getApplicationStatusDisplay(UserRole.ApplicationStatus status) {
//        return switch (status) {
//            case PENDING -> "⏳ На рассмотрении";
//            case ACCEPTED -> "✅ Принят";
//            case REJECTED -> "❌ Отклонен";
//            case WITHDRAWN -> "↩️ Отозван";
//            default -> "❓ Неизвестно";
//        };
//    }
//
//    // 🔥 ДОБАВЛЯЕМ В ProjectsHandler
//    private String getApplicationStatusIcon(UserRole.ApplicationStatus status) {
//        return switch (status) {
//            case PENDING -> "⏳ ";
//            case ACCEPTED -> "✅ ";
//            case REJECTED -> "❌ ";
//            case WITHDRAWN -> "↩️ ";
//            default -> "📄 ";
//        };
//    }
//
//    public int getProjectsPerPage() {
//        return PROJECTS_PER_PAGE;
//    }
//
//    private int getPageSizeForContext(String contextKey) {
//        switch (contextKey) {
//            case PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY:
//                return APPLICATIONS_PER_PAGE;
//            case PaginationContextKeys.MY_PROJECTS_CONTEXT_KEY:
//                return 3; // специальный размер для моих проектов
//            default:
//                return PROJECTS_PER_PAGE;
//        }
//    }
//}
//
//
