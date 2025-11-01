package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.dto.ProjectData;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProjectsHandler extends BaseHandler {

    private final ProjectViewService projectViewService;
    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final ProjectSearchService projectSearchService;
    private final ApplicationHandler applicationHandler;
    private final RoleBasedMenuService roleBasedMenuService;
    private int delaySeconds;

    // 🔥 MAP ДЛЯ ХРАНЕНИЯ ID СООБЩЕНИЙ С ПРОЕКТАМИ

    public ProjectsHandler(KeyboardFactory keyboardFactory, ProjectViewService projectViewService,
                           ProjectService projectService, ApplicationService applicationService,
                           ProjectSearchService projectSearchService, ApplicationHandler applicationHandler, UserSessionService userSessionService, RoleBasedMenuService roleBasedMenuService) {
        super(keyboardFactory, userSessionService);
        this.projectViewService = projectViewService;
        this.projectService = projectService;
        this.applicationService = applicationService;
        this.projectSearchService = projectSearchService;
        this.applicationHandler = applicationHandler;
        this.roleBasedMenuService = roleBasedMenuService;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "projects".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        ProjectData data = new ProjectData(chatId, messageId, userName, null, action, parameter);

        switch (action) {
            case "menu":
                showProjectsMenu(data);
                break;
            case "my_projects":
                showMyProjectsMenu(data);
                break;
            case "my_list":
                showMyProjectsList(data, parameter);
                break;
            case "favorites":
                showFavorites(data);
                break;
            case "applications":
                if (parameter != null) {
                    // 🔥 ОТКЛИКИ НА КОНКРЕТНЫЙ ПРОЕКТ (projects:applications:123)
                    showProjectApplications(data, parameter);
                } else {
                    // 🔥 МОИ ОТКЛИКИ КАК ИСПОЛНИТЕЛЬ (projects:applications)
                    showMyApplications(data);
                }
                break;
            case "active":
                showActiveProjects(data);
                break;
            case "search":
                showProjectSearch(data);
                break;
            case "details":
                showProjectDetail(data);
                break;
            case "clear_search":
                clearSearchResult(data);
                break;
            case "filter":
                handleSearchFilter(data, parameter);
                break;
            case "pagination":
                handlePagination(data, parameter);
                break;
            default:
                log.warn("❌ Unknown projects action: {}", action);
        }
    }

    public void showProjectsMenu(ProjectData data) {
        String text = """
            💼 **РАЗДЕЛ ПРОЕКТОВ TCMatch**
            
            Выберите нужный раздел:
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectsMenuKeyboard(data.getChatId() );
        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
    }

    public void showMyProjectsMenu(ProjectData data) {
        UserRole userRole = roleBasedMenuService.getUserRole(data.getChatId());

        if (userRole == UserRole.CUSTOMER) {
            String text = """
                👔 **МОИ ПРОЕКТЫ**
                
                Управление вашими проектами:
                """;
            InlineKeyboardMarkup keyboard = roleBasedMenuService.createMyProjectsMenu(data.getChatId());
            editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
        } else {
            String text = """
                👨‍💻 **УПРАВЛЕНИЕ ЗАКАЗАМИ**
                
                📊 Этот раздел доступен только заказчикам
                
                💡 Для исполнителей доступны:
                • ⚙️ Выполняемые - ваши активные заказы
                • 📨 Откликнутые - проекты, куда вы откликнулись
                • 🔍 Поиск проектов - находите новые проекты
                """;
            InlineKeyboardMarkup keyboard = roleBasedMenuService.createMyProjectsMenu(data.getChatId());
            editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
        }
    }

    private void showMyProjectsList(ProjectData data, String statusFilter) {
        try {
            List<Project> projects = projectService.getUserProjects(data.getChatId());
            if (statusFilter != null && !"all".equals(statusFilter)) {
                UserRole.ProjectStatus status = UserRole.ProjectStatus.valueOf(statusFilter.toUpperCase());
                projects = projects.stream()
                        .filter(p -> p.getStatus() == status)
                        .collect(Collectors.toList());
            }

            if (projects.isEmpty()) {
                String text = """
                    📭 **ПРОЕКТЫ НЕ НАЙДЕНЫ**
                    
                    💡 Создайте первый проект чтобы найти исполнителя
                    """;
                editMessage(data.getChatId(), data.getMessageId(), text,
                        keyboardFactory.createBackToMyProjectsKeyboard());
                return;
            }

            userSessionService.putToContext(data.getChatId(), "my_projects_list", projects);
            userSessionService.putToContext(data.getChatId(), "my_projects_page", 0);
            userSessionService.putToContext(data.getChatId(), "my_projects_filter", statusFilter);

            showCustomerProjectsPage(data, projects, 0, statusFilter);

        } catch (Exception e) {
            log.error("❌ Ошибка показа списка проектов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки проектов", 5);
        }
    }

    private void showCustomerProjectsPage(ProjectData data, List<Project> projects, int page, String filter) {
        int pageSize = 3;
        int totalPages = (int) Math.ceil((double) projects.size() / pageSize);
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, projects.size());

        String filterDisplay = getFilterDisplay(filter);

        StringBuilder text = new StringBuilder("""
            👔 **ВАШИ ПРОЕКТЫ**
            
            📊 %s | Страница %d из %d
            """.formatted(filterDisplay, page + 1, totalPages));

        for (int i = startIndex; i < endIndex; i++) {
            Project project = projects.get(i);
            text.append("""
                
                %s%s
                💰 %.0f руб | ⏱️ %d дн. | %s
                👀 %d просмотров | 📨 %d откликов
                """.formatted(
                    getProjectStatusIcon(project.getStatus()),
                    project.getTitle(),
                    project.getBudget(),
                    project.getEstimatedDays(),
                    getProjectStatusDisplay(project.getStatus()),
                    project.getViewsCount(),
                    project.getApplicationsCount()
            ));
        }

        InlineKeyboardMarkup keyboard = keyboardFactory.createCustomerProjectsListKeyboard(
                projects, page, totalPages, filter);

        editMessage(data.getChatId(), data.getMessageId(), text.toString(), keyboard);
    }

    // 🔥 ОТКЛИКИ НА ПРОЕКТ (для заказчика)
    private void showProjectApplications(ProjectData data, String projectId) {
        try {
            Long projectIdLong = Long.parseLong(projectId);
            List<Application> applications = applicationService.getProjectApplications(projectIdLong);

            if (applications.isEmpty()) {
                String text = """
                    📭 **ОТКЛИКОВ НЕТ**
                    
                    💡 На ваш проект еще никто не откликнулся
                    """;
                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
                return;
            }

            showApplicationsForProject(data, applications, projectIdLong);

        } catch (Exception e) {
            log.error("❌ Ошибка показа откликов на проект: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки откликов", 5);
        }
    }

    // 🔥 ОТОБРАЖЕНИЕ ОТКЛИКОВ НА ПРОЕКТ
    private void showApplicationsForProject(ProjectData data, List<Application> applications, Long projectId) {
        StringBuilder text = new StringBuilder("""
            📨 **ОТКЛИКИ НА ПРОЕКТ**
            
            """);

        for (int i = 0; i < Math.min(applications.size(), 10); i++) {
            Application app = applications.get(i);
            text.append("""
                %d. 👨‍💻 *%s*
                   💰 Предложил: %.0f руб
                   ⏱️ Срок: %d дней
               📊 Рейтинг: ⭐ %.1f
                   📅 Отправлен: %s
                
                """.formatted(
                    i + 1,
                    app.getFreelancer().getUsername() != null ?
                            "@" + app.getFreelancer().getUsername() : "Пользователь",
                    app.getProposedBudget(),
                    app.getProposedDays(),
                    app.getFreelancer().getProfessionalRating(),
                    app.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ));
        }

        if (applications.size() > 10) {
            text.append("\n📊 ... и еще ").append(applications.size() - 10).append(" откликов");
        }

        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectApplicationsKeyboard(projectId);
        editMessage(data.getChatId(), data.getMessageId(), text.toString(), keyboard);
    }

    // 🔥 ОБНОВЛЯЕМ showProjectDetail - добавляем поддержку applicationId
    public void showProjectDetail(ProjectData data) {
        try {
            Long projectId;
            String parameter = data.getParameter();

            // 🔥 ПРОВЕРЯЕМ - ПЕРЕДАН ID ПРОЕКТА ИЛИ ID ОТКЛИКА?
            if (parameter.startsWith("app_")) {
                // 🔥 ЕСЛИ ПЕРЕДАН ID ОТКЛИКА (app_123) - ПОЛУЧАЕМ ID ПРОЕКТА
                Long applicationId = Long.parseLong(parameter.replace("app_", ""));
                projectId = applicationService.getProjectIdByApplicationId(applicationId);
            } else {
                // 🔥 ЕСЛИ ПЕРЕДАН ОБЫЧНЫЙ ID ПРОЕКТА
                projectId = Long.parseLong(parameter);
            }

            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            deletePreviousProjectMessages(data.getChatId());

            // 🔥 РЕГИСТРИРУЕМ ПРОСМОТР ТОЛЬКО ЗДЕСЬ - КОГДА ПОЛЬЗОВАТЕЛЬ ДЕЙСТВИТЕЛЬНО СМОТРИТ ПРОЕКТ
            projectViewService.registerProjectView(data.getChatId(), projectId);

            String projectText = formatProjectDetails(project);

            boolean canApply = roleBasedMenuService.canUserApplyToProjects(data.getChatId()) &&
                    !roleBasedMenuService.isProjectOwner(data.getChatId(), project.getCustomer().getId());

            InlineKeyboardMarkup keyboard = roleBasedMenuService.createProjectDetailsKeyboard(
                    data.getChatId(), projectId, canApply);

            Integer mainMessageId = getMainMessageId(data.getChatId());

            if (mainMessageId != null) {
                editMessage(data.getChatId(), mainMessageId, projectText, keyboard);
            } else {
                Integer newMessageId = sendInlineMessageReturnId(data.getChatId(), projectText, keyboard);
                saveMainMessageId(data.getChatId(), newMessageId);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка показа деталей проекта: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки информации о проекте", 5);
        }
    }

    // 🔥 ПАГИНАЦИЯ "МОИХ ПРОЕКТОВ"
    private void handleMyProjectsPagination(ProjectData data, String parameter) {
        try {
            String[] parts = parameter.split(":");
            String direction = parts[0];
            String filter = parts[2];

            List<Project> projects = userSessionService.getFromContext(data.getChatId(),
                    "my_projects_list", List.class);
            Integer currentPage = userSessionService.getFromContext(data.getChatId(),
                    "my_projects_page", Integer.class);

            if (projects == null || currentPage == null) {
                showMyProjectsList(data, filter);
                return;
            }

            int totalPages = (int) Math.ceil((double) projects.size() / 3);
            int newPage = currentPage;

            if ("next".equals(direction) && currentPage < totalPages - 1) {
                newPage = currentPage + 1;
            } else if ("prev".equals(direction) && currentPage > 0) {
                newPage = currentPage - 1;
            }

            userSessionService.putToContext(data.getChatId(), "my_projects_page", newPage);
            showCustomerProjectsPage(data, projects, newPage, filter);

        } catch (Exception e) {
            log.error("❌ Ошибка пагинации моих проектов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
        }
    }

    public void showFavorites(ProjectData data) {
        try {
            List<Project> favoriteProjects = getFavoriteProjects(data.getChatId());

            if (favoriteProjects.isEmpty()) {
                String text = """
                        ❤️ **ИЗБРАННЫЕ ПРОЕКТЫ**
                        
                        📭 У вас пока нет избранных проектов
                        
                        💡 *Как добавить в избранное:*
                        • Находите интересный проект в поиске
                        • Нажимайте кнопку "⭐ В избранное"
                        • Возвращайтесь к нему позже
                        """;
                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
                return;
            }

            // Показываем первый проект из избранного с пагинацией
            showProjectWithPagination(data, favoriteProjects, 0, "favorites");
        } catch (Exception e) {
            log.error("❌ Ошибка показа избранных проектов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки избранных проектов", 5);
        }
    }

    public void showMyApplications(ProjectData data) {
        try {
            // 🔥 РЕАЛЬНАЯ ЛОГИКА - получение откликов пользователя
            List<Application> userApplications = applicationService.getUserApplications(data.getChatId());

            if (userApplications.isEmpty()) {
                String text = """
                        📨 **ОТКЛИКНУТНЫЕ ПРОЕКТЫ**
                        
                        📭 Вы еще не откликались на проекты
                        
                        💡 *Как найти проекты:*
                        • Используйте поиск проектов
                        • Изучите требования заказчиков
                        • Отправляйте качественные отклики
                        """;
                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
                return;
            }

            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ С ОТКЛИКАМИ
            deletePreviousProjectMessages(data.getChatId());

            // 🔥 СОХРАНЯЕМ MESSAGE_ID ЕСЛИ ЕЩЁ НЕТ
            if (getMainMessageId(data.getChatId()) == null) {
                saveMainMessageId(data.getChatId(), data.getMessageId());
            }

// 🔥 СОХРАНЯЕМ ДЛЯ ПАГИНАЦИИ
            userSessionService.putToContext(data.getChatId(), "my_applications_list", userApplications);
            userSessionService.putToContext(data.getChatId(), "my_applications_page", 0);

            // 🔥 ПОКАЗЫВАЕМ ПЕРВУЮ СТРАНИЦУ
            showApplicationsPage(data, userApplications, 0);

        } catch (Exception e) {
            log.error("❌ Ошибка показа откликов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки ваших откликов", 5);
        }
    }

    private void showApplicationsPage(ProjectData data, List<Application> applications, int page) {
        try {
            int pageSize = 5; // 5 откликов на страницу
            int startIndex = page * pageSize;
            int endIndex = Math.min(startIndex + pageSize, applications.size());

            List<Application> pageApplications = applications.subList(startIndex, endIndex);

            // 🔥 ОТПРАВЛЯЕМ КАЖДЫЙ ОТКЛИК ОТДЕЛЬНЫМ СООБЩЕНИЕМ
            List<Integer> newMessageIds = new ArrayList<>();

            for (int i = 0; i < pageApplications.size(); i++) {
                Application application = pageApplications.get(i);
                String applicationText = formatApplicationPreview(application, startIndex + i + 1);

                // 🔥 КЛАВИАТУРА ДЛЯ ОТКЛИКА
                InlineKeyboardMarkup applicationKeyboard = keyboardFactory.createApplicationItemKeyboard(
                        application.getId(),
                        application.getStatus()
                );

                Integer newMessageId = sendHtmlMessageReturnId(data.getChatId(), applicationText, applicationKeyboard);
                if (newMessageId != null) {
                    newMessageIds.add(newMessageId);
                }
            }

            // 🔥 ОТПРАВЛЯЕМ ПАГИНАЦИЮ КАК ОТДЕЛЬНОЕ СООБЩЕНИЕ
            String paginationText = createApplicationsPaginationText(applications, page);
            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createApplicationsPaginationKeyboard(page, applications.size());

            Integer paginationMessageId = sendInlineMessageReturnId(data.getChatId(), paginationText, paginationKeyboard);
            if (paginationMessageId != null) {
                newMessageIds.add(paginationMessageId);
            }

            // 🔥 СОХРАНЯЕМ ID НОВЫХ СООБЩЕНИЙ
            saveProjectMessageIds(data.getChatId(), newMessageIds);

            // 🔥 ОБНОВЛЯЕМ ГЛАВНОЕ СООБЩЕНИЕ
            String controlText = """
            📨 **ВАШИ ОТКЛИКИ**
            
            💼 Всего откликов: %d
            """.formatted(
                    applications.size(),
                    page + 1,
                    (int) Math.ceil((double) applications.size() / 5)
            );

            InlineKeyboardMarkup controlKeyboard = keyboardFactory.createApplicationsControlKeyboard();
            editMessage(data.getChatId(), data.getMessageId(), controlText, controlKeyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка показа страницы откликов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки откликов", 5);
        }
    }

    // 🔥 ФОРМАТИРОВАНИЕ ПРЕВЬЮ ОТКЛИКА
    private String formatApplicationPreview(Application application, int number) {
        Project project = application.getProject();

        return """
        <b>📨 **Отклик #%d**</b>
        
        <blockquote><b>💼 *Проект:* %s</b>
        <b>💰 *Ваше предложение:* %.0f руб</b>
        <b>⏱️ *Срок:* %d дней</b>
        <b>📅 *Отправлен:* %s</b>
        <b>📊 *Статус:* %s</b>
        
        <b>📝 *Ваше сообщение:*</b>
        <i>%s</i></blockquote>
        """.formatted(
                number,
                project.getTitle(),
                application.getProposedBudget(),
                application.getProposedDays(),
                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                getApplicationStatusDisplay(application.getStatus()),
                application.getCoverLetter().length() > 150 ?
                        application.getCoverLetter().substring(0, 150) + "..." :
                        application.getCoverLetter()
        );
    }

    // 🔥 ТЕКСТ ПАГИНАЦИИ ДЛЯ ОТКЛИКОВ
    private String createApplicationsPaginationText(List<Application> applications, int page) {
        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) applications.size() / pageSize);
        int startApplication = (page * pageSize) + 1;
        int endApplication = Math.min((page + 1) * pageSize, applications.size());

        return """
        📄 **СТРАНИЦА %d ИЗ %d**
        
        📊 Показаны отклики: %d-%d из %d
        
        🔽 Используйте кнопки ниже для навигации:
        """.formatted(
                page + 1,
                totalPages,
                startApplication,
                endApplication,
                applications.size()
        );
    }


    // 🔥 ПАГИНАЦИЯ ДЛЯ ОТКЛИКОВ
    private void handleApplicationsPagination(ProjectData data, String direction) {
        try {
            List<Application> applications = userSessionService.getFromContext(data.getChatId(),
                    "my_applications_list", List.class);
            Integer currentPage = userSessionService.getFromContext(data.getChatId(),
                    "my_applications_page", Integer.class);

            if (applications == null || currentPage == null) {
                showMyApplications(data);
                return;
            }

            int totalPages = (int) Math.ceil((double) applications.size() / 5);
            int newPage = currentPage;

            if ("next".equals(direction) && currentPage < totalPages - 1) {
                newPage = currentPage + 1;
            } else if ("prev".equals(direction) && currentPage > 0) {
                newPage = currentPage - 1;
            }

            userSessionService.putToContext(data.getChatId(), "my_applications_page", newPage);

            // 🔥 УДАЛЯЕМ СТАРЫЕ СООБЩЕНИЯ И ПОКАЗЫВАЕМ НОВУЮ СТРАНИЦУ
            deletePreviousProjectMessages(data.getChatId());
            showApplicationsPage(data, applications, newPage);

        } catch (Exception e) {
            log.error("❌ Ошибка пагинации откликов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
        }
    }

    public void showActiveProjects(ProjectData data) {
        try {
            // 🔥 РЕАЛЬНАЯ ЛОГИКА - получение активных проектов пользователя
            List<Project> activeProjects = projectService.getFreelancerProjects(data.getChatId())
                    .stream()
                    .filter(p -> p.getStatus() == UserRole.ProjectStatus.IN_PROGRESS)
                    .collect(Collectors.toList());

            if (activeProjects.isEmpty()) {
                String text = """
                    ⚙️ **ВЫПОЛНЯЕМЫЕ ПРОЕКТЫ**
                    
                    📊 Сейчас у вас нет активных проектов
                    
                    💡 *Как получить заказы:*
                    • Активно откликайтесь на проекты
                    • Следите за своим рейтингом
                    • Предлагайте конкурентные условия
                    """;
                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
                return;
            }

            // Показываем активные проекты
            showActiveProjectsList(data, activeProjects);

        } catch (Exception e) {
            log.error("❌ Ошибка показа активных проектов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки активных проектов", delaySeconds);
        }
    }

    public void showProjectSearch(ProjectData data) {
        try {
            String filter = data.getFilter() != null ? data.getFilter() : "";

            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ ПЕРЕД НОВЫМ ПОИСКОМ
            deletePreviousProjectMessages(data.getChatId());

            // 🔥 ЕСЛИ У НАС ЕЩЁ НЕТ СОХРАНЕННОГО MESSAGE_ID - СОХРАНЯЕМ ЕГО
            if (getMainMessageId(data.getChatId()) == null) {
                saveMainMessageId(data.getChatId(), data.getMessageId());
            }

            // 🔥 ВСЕГДА ИСПОЛЬЗУЕМ СОХРАНЕННЫЙ MESSAGE_ID
            Integer mainMessageId = getMainMessageId(data.getChatId());

            // 🔥 ЕСЛИ ФИЛЬТР ПУСТОЙ - ПОКАЗЫВАЕМ ТОЛЬКО ИНТЕРФЕЙС ПОИСКА
            if (filter.isEmpty()) {
                String text = """
                🔍 **ПОИСК ПРОЕКТОВ TCMatch**
                
                🚀 *Выберите фильтр для начала поиска*
                """;

                InlineKeyboardMarkup keyboard = keyboardFactory.createSearchControlKeyboard(filter);
                editMessage(data.getChatId(), mainMessageId, text, keyboard);
                return;
            }

            ProjectSearchService.SearchState searchState = projectSearchService.getOrCreateSearchState(data.getChatId(), filter);
            List<Project> searchResults = searchState.projects;
            if (searchResults.isEmpty()) {
                String text = """
                    🔍 **ПРОЕКТЫ НЕ НАЙДЕНЫ**
                    
                    💡 Попробуйте:
                    • Изменить фильтры поиска
                    • Расширить критерии поиска
                    • Проверить позже
                    """;
                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createSearchControlKeyboard(filter));
                return;
            }

            List<Project> pageProjects = projectSearchService.getPageProjects(data.getChatId(), filter);

            log.debug("🔍 DEBUG: pageProjects.size() = {}, currentPage = {}",
                    pageProjects.size(), searchState.currentPage);

            // 🔥 ОТПРАВЛЯЕМ КАЖДЫЙ ПРОЕКТ ОТДЕЛЬНЫМ СООБЩЕНИЕМ
            List<Integer> newMessageIds = new ArrayList<>();
            for (int i = 0; i < pageProjects.size(); i++) {
                Project project = pageProjects.get(i);
                String projectText = formatProjectPreview(project, i + 1);
                Integer start = projectText.indexOf("💼");
                Integer end = projectText.length();

                // 🔥 УПРОЩЕННАЯ КЛАВИАТУРА - ТОЛЬКО "ДЕТАЛИ"
                InlineKeyboardMarkup projectKeyboard = keyboardFactory.createProjectPreviewKeyboard(project.getId());

                Integer newMessageId = sendInlineMessageWithQuoteReturnId(data.getChatId(), projectText, "💼", (end - start), projectKeyboard);
                if (newMessageId != null) {
                    newMessageIds.add(newMessageId);
                }
            }

            // 🔥 ОТПРАВЛЯЕМ ПАГИНАЦИЮ КАК ОТДЕЛЬНОЕ СООБЩЕНИЕ ПОСЛЕ ПРОЕКТОВ
            String paginationText = createPaginationText(data.getChatId(), searchState);
            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboard(filter, data.getChatId());

            Integer paginationMessageId = sendInlineMessageReturnId(data.getChatId(), paginationText, paginationKeyboard);
            if (paginationMessageId != null) {
                newMessageIds.add(paginationMessageId);
            }

            // 🔥 СОХРАНЯЕМ ID НОВЫХ СООБЩЕНИЙ
            saveProjectMessageIds(data.getChatId(), newMessageIds);

            // 🔥 ОТПРАВЛЯЕМ СООБЩЕНИЕ С ПАГИНАЦИЕЙ И УПРАВЛЕНИЕМ
            String controlText = """
            📊 **РЕЗУЛЬТАТЫ ПОИСКА**
            
            💼 Найдено проектов: %d
            
            💡 Используйте фильтры для уточнения поиска
            """.formatted(
                    searchResults.size(),
                    searchState.currentPage + 1
            );

            InlineKeyboardMarkup controlKeyboard = keyboardFactory.createSearchControlKeyboard(filter);
            editMessage(data.getChatId(), data.getMessageId(), controlText, controlKeyboard);
        } catch (Exception e) {
            log.error("❌ Ошибка поиска проектов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка поиска проектов", 5);
        }
    }

    private  String createPaginationText(Long chatId, ProjectSearchService.SearchState state) {
        int totalPages = (int) Math.ceil((double) state.projects.size() / state.pageSize);
        int startProject = (state.currentPage * state.pageSize) + 1;
        int endProject = Math.min((state.currentPage + 1) * state.pageSize, state.projects.size());

        return """
        📄 **СТРАНИЦА %d ИЗ %d**
        
        📊 Показаны проекты: %d-%d из %d
        
        🔽 Используйте кнопки ниже для навигации:
        """.formatted(
                state.currentPage + 1,
                totalPages,
                startProject,
                endProject,
                state.projects.size()
        );
    }

    // 🔥 СПЕЦИАЛЬНЫЙ ФОРМАТ ДЛЯ ОТКЛИКА
    private String formatProjectDetailsForApplication(Project project) {
        return """
        📝 **ОТКЛИК НА ПРОЕКТ**
        
        💼 *Название проекта:* %s
        💰 *Бюджет:* %.0f руб
        ⏱️ *Срок выполнения:* %d дней
        📅 *Дедлайн:* %s
        
        📊 *Статистика проекта:*
        👀 Просмотров: %d
        📨 Откликов: %d
        
        👔 *Заказчик:* @%s
        ⭐ *Рейтинг заказчика:* %.1f/5.0
        
        📝 *Описание проекта:*
        %s
        
        🛠️ *Требуемые навыки:*
        %s
        
        💡 *Для отклика нажмите кнопку ниже*
        """.formatted(
                project.getTitle(),
                project.getBudget(),
                project.getEstimatedDays(),
                project.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                project.getViewsCount(),
                project.getApplicationsCount(),
                project.getCustomer().getUsername() != null ? project.getCustomer().getUsername() : "скрыт",
                project.getCustomer().getProfessionalRating(),
                project.getDescription(),
                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны"
        );
    }

    public void clearSearchResult(ProjectData data) {
        deletePreviousProjectMessages(data.getChatId());

        String text = """
        🗑️ **РЕЗУЛЬТАТЫ ОЧИЩЕНЫ**
        
        💡 Что дальше:
        • Начните новый поиск
        • Используйте фильтры
        • Вернитесь в меню проектов
        """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createSearchStartKeyboard();
        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
    }

    public void handleSearchFilter(ProjectData data, String filter) {
        try {

            // 🔥 ОЧИЩАЕМ ТЕКУЩИЙ ПОИСК ДЛЯ ПРИМЕНЕНИЯ НОВОГО ФИЛЬТРА
            projectSearchService.clearSearchState(data.getChatId());

            // 🔥 СОЗДАЕМ НОВЫЙ ProjectData С ФИЛЬТРОМ
            ProjectData filteredData = new ProjectData(
                    data.getChatId(),
                    data.getMessageId(),
                    data.getUserName(),
                    filter,
                    "search",
                    null
            );

            // 🔥 ЗАПУСКАЕМ ПОИСК С НОВЫМ ФИЛЬТРОМ
            showProjectSearch(filteredData);
        } catch (Exception e) {
            log.error("❌ Ошибка применения фильтра: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка применения фильтра", 5);
        }
    }

    private void handlePagination(ProjectData data, String parameter) {
        try {
            // 🔥 ПАГИНАЦИЯ ДЛЯ ОТКЛИКОВ
            if (parameter.startsWith("applications:")) {
                handleApplicationsPagination(data, parameter.replace("applications:", ""));
                return;
            }

            // 🔥 ПАГИНАЦИЯ ДЛЯ "МОИХ ПРОЕКТОВ"
            if (parameter.startsWith("my_list:")) {
                handleMyProjectsPagination(data, parameter);
                return;
            }

            String[] parts = parameter.split(":", 2); // Разбиваем на 2 части максимум
            String direction = parts[0];
            String filter = (parts.length > 1 && !parts[1].isEmpty()) ? parts[1] : "";

            if ("next".equals(direction)) {
                projectSearchService.nextPage(data.getChatId());
            } else if ("prev".equals(direction)) {
                projectSearchService.prevPage(data.getChatId());
            }

            ProjectData searchData = new ProjectData(data.getChatId(), data.getMessageId(), data.getUserName(), filter, "search", null);
            showProjectSearch(searchData);
        } catch (Exception e) {
            log.error("❌ Ошибка пагинации: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка переключения страницы", 5);
        }
    }

    private List<Project> getFavoriteProjects(Long chatId) {
        // Временная заглушка - потом реализуем настоящую логику избранного
        return List.of();
    }

    private void showProjectWithPagination(ProjectData data, List<Project> projects, int currentIndex, String context) {
        if (projects.isEmpty() || currentIndex >= projects.size()) return;

        Project project = projects.get(currentIndex);
        String projectText = formatProjectPreview(project, currentIndex + 1);
        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectWithPaginationKeyboard(
                project.getId(), currentIndex, projects.size(), context
        );

        editMessage(data.getChatId(), data.getMessageId(), projectText, keyboard);
    }

    private void showApplicationsList(ProjectData data, List<Application> applications) {
        StringBuilder text = new StringBuilder("📨 **ВАШИ ОТКЛИКИ**\n\n");

        for (int i = 0; i < Math.min(applications.size(), 10); i++) {
            Application app = applications.get(i);
            Project project = app.getProject();

            text.append("""
                    %d. 💼 *%s*
                       💰 Бюджет: %.0f руб
                       ⏱️ Срок: %d дней
                       📊 Статус: %s
                       📅 Отправлен: %s
                    
                    """.formatted(
                    i + 1,
                    project.getTitle(),
                    project.getBudget(),
                    project.getEstimatedDays(),
                    getApplicationStatusDisplay(app.getStatus()),
                    app.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ));
        }

        editMessage(data.getChatId(), data.getMessageId(), text.toString(), keyboardFactory.createBackButton());
    }

    private String formatProjectDetails(Project project) {
        return """
            💼 **ДЕТАЛИ ПРОЕКТА**
            
            🎯 *Название:* %s
            💰 *Бюджет:* %.0f руб
            ⏱️ *Срок:* %d дней
            📅 *Дедлайн:* %s
            👀 *Просмотров:* %d
            📨 *Откликов:* %d
            
            📝 *Описание:*
            %s
            
            🛠️ *Требуемые навыки:*
            %s
            
            👔 *Заказчик:* @%s
            📊 *Рейтинг заказчика:* ⭐ %.1f/5.0
            """.formatted(
                project.getTitle(),
                project.getBudget(),
                project.getEstimatedDays(),
                project.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                project.getViewsCount(),
                project.getApplicationsCount(),
                project.getDescription(),
                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны",
                project.getCustomer().getUsername() != null ? project.getCustomer().getUsername() : "скрыт",
                project.getCustomer().getProfessionalRating()
        );
    }

    private String formatProjectPreview(Project project, int number) {
        return """
            🎯 **Проект #%d**
            
            💼 *%s*
            💰 Бюджет: *%.0f руб*
            ⏱️ Срок: *%d дней*
            👀 Просмотров: *%d*
            📨 Откликов: *%d*
            
            📝 %s
            """.formatted(
                number,
                project.getTitle(),
                project.getBudget(),
                project.getEstimatedDays(),
                project.getViewsCount(),
                project.getApplicationsCount(),
                project.getDescription().length() > 100 ?
                        project.getDescription().substring(0, 100) + "..." :
                        project.getDescription()
        );
    }

    private void showActiveProjectsList(ProjectData data, List<Project> activeProjects) {
        String text = "🚧 Раздел 'Выполняемые' в разработке...";
        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
    }

    private String getFilterDisplay(String filter) {
        return switch (filter) {
            case "all" -> "Все проекты";
            case "open" -> "Открытые";
            case "in_progress" -> "В работе";
            case "completed" -> "Завершенные";
            default -> "Проекты";
        };
    }

    private String getProjectStatusIcon(UserRole.ProjectStatus status) {
        return switch (status) {
            case OPEN -> "🔓 ";
            case IN_PROGRESS -> "⚙️ ";
            case COMPLETED -> "✅ ";
            case CANCELLED -> "❌ ";
            default -> "📁 ";
        };
    }

    private String getProjectStatusDisplay(UserRole.ProjectStatus status) {
        return switch (status) {
            case OPEN -> "Открыт";
            case IN_PROGRESS -> "В работе";
            case COMPLETED -> "Завершен";
            case CANCELLED -> "Отменен";
            default -> "Неизвестно";
        };
    }

    private String getApplicationStatusDisplay(UserRole.ApplicationStatus status) {
        return switch (status) {
            case PENDING -> "⏳ На рассмотрении";
            case ACCEPTED -> "✅ Принят";
            case REJECTED -> "❌ Отклонен";
            case WITHDRAWN -> "↩️ Отозван";
            default -> "❓ Неизвестно";
        };
    }

    // 🔥 ДОБАВЛЯЕМ В ProjectsHandler
    private String getApplicationStatusIcon(UserRole.ApplicationStatus status) {
        return switch (status) {
            case PENDING -> "⏳ ";
            case ACCEPTED -> "✅ ";
            case REJECTED -> "❌ ";
            case WITHDRAWN -> "↩️ ";
            default -> "📄 ";
        };
    }
}


