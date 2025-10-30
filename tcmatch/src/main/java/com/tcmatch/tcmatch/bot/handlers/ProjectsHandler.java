package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.dto.ProjectData;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.NavigationService;
import com.tcmatch.tcmatch.service.ProjectSearchService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProjectsHandler extends BaseHandler {

    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final ProjectSearchService projectSearchService;
    private final ApplicationHandler applicationHandler;
    private int delaySeconds;

    // 🔥 MAP ДЛЯ ХРАНЕНИЯ ID СООБЩЕНИЙ С ПРОЕКТАМИ

    public ProjectsHandler(KeyboardFactory keyboardFactory, NavigationService navigationService,
                           ProjectService projectService, ApplicationService applicationService,
                           ProjectSearchService projectSearchService, ApplicationHandler applicationHandler) {
        super(keyboardFactory, navigationService);
        this.projectService = projectService;
        this.applicationService = applicationService;
        this.projectSearchService = projectSearchService;
        this.applicationHandler = applicationHandler;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "projects".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        ProjectData data = new ProjectData(chatId, messageId, userName, null, action, parameter);

        switch (action) {
            case "show_menu":
                showProjectsMenu(data);
                break;
            case "favorites":
                showFavorites(data);
                break;
            case "applications":
                showMyApplications(data);
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

        InlineKeyboardMarkup keyboard = keyboardFactory.createProjectsMenuKeyboard();
        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
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

            // Показываем проекты, куда пользователь откликался
            showApplicationsList(data, userApplications);
        } catch (Exception e) {
            log.error("❌ Ошибка показа откликов: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки ваших откликов", 5);
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

    public void showProjectDetail(ProjectData data) {
        try {
            Long projectId = Long.parseLong(data.getParameter());
            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            String projectText = formatProjectDetails(project);
            InlineKeyboardMarkup keyboard = keyboardFactory.createProjectDetailsKeyboard(projectId, true);
            Integer start = projectText.indexOf("🎯 *Название:");
            Integer end = projectText.indexOf("👔 *Заказчик:");

            editMessageWithQuote(data.getChatId(), data.getMessageId(), projectText, "🎯 *Название:", (end - start), keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка показа деталей проекта: {}", e.getMessage());
            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки информации о проекте", 5);
        }
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

    private String getApplicationStatusDisplay(UserRole.ApplicationStatus status) {
        return switch (status) {
            case PENDING -> "⏳ На рассмотрении";
            case ACCEPTED -> "✅ Принят";
            case REJECTED -> "❌ Отклонен";
            case WITHDRAWN -> "↩️ Отозван";
            default -> "❓ Неизвестно";
        };
    }

    private void showActiveProjectsList(ProjectData data, List<Project> activeProjects) {
        return;
    }

}


