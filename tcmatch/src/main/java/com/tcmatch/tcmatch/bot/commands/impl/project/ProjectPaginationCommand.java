package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.PROJECTS_PER_PAGE;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectPaginationCommand implements Command {

    private final BotExecutor botExecutor;
    private final PaginationManager paginationManager;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final ProjectService projectService;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "pagination".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            // Формат: "next:favorites:PROJECT" или "next:project_search:PROJECT" или "next:customer_projects:PROJECT"
            String[] parts = context.getParameter().split(":");
            if (parts.length < 3) return;

            String direction = parts[0];
            String contextKey = parts[1];
            String entityType = parts[2];

            // 🔥 ОПРЕДЕЛЯЕМ РЕНДЕРЕР ДЛЯ КОНТЕКСТА
            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = null;
            int pageSize = PROJECTS_PER_PAGE;

            if (PaginationContextKeys.PROJECT_FAVORITES_CONTEXT_KEY.equals(contextKey)) {
                renderer = this::renderFavoritesPage;
            } else if (PaginationContextKeys.PROJECT_SEARCH_CONTEXT_KEY.equals(contextKey)) {
                renderer = this::renderSearchPage;
            } else if (PaginationContextKeys.MY_PROJECTS_CONTEXT_KEY.equals(contextKey)) {
                renderer = this::renderCustomerProjectsPage;
            }

            if (renderer == null) {
                log.error("❌ Renderer not found for project context: {}", contextKey);
                return;
            }

            // 🔥 ВЫЗЫВАЕМ PAGINATION MANAGER
            paginationManager.renderIdBasedPage(
                    context.getChatId(),
                    contextKey,
                    null, // ID уже в контексте
                    entityType,
                    direction,
                    pageSize,
                    renderer
            );

        } catch (Exception e) {
            log.error("❌ Ошибка пагинации проектов: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка переключения страницы", 5);
        }
    }

    public List<Integer> renderCustomerProjectsPage(List<Long> pageProjectIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        // Получаем проекты по ID
        List<ProjectDto> pageProjects = projectService.getProjectsByIds(pageProjectIds);

        // Отправляем карточки проектов
        for (int i = 0; i < pageProjects.size(); i++) {
            ProjectDto project = pageProjects.get(i);
            String projectText = formatCustomerProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);

            // Клавиатура для карточки проекта
            InlineKeyboardMarkup projectKeyboard = projectKeyboards.createProjectPreviewKeyboard(project.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, projectText, projectKeyboard);
            if (cardId != null) messageIds.add(cardId);
        }

        // Пагинация
        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);
        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);

        return messageIds;
    }

    public List<Integer> renderSearchPage(List<Long> pageProjectIds, PaginationContext context) {
        Long chatId = context.chatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        List<Integer> messageIds = new ArrayList<>();

        // Получаем проекты по ID
        List<ProjectDto> pageProjects = projectService.getProjectsByIds(pageProjectIds);

        botExecutor.editMessageWithHtml(chatId, messageId, "<b>🔍Найдено проектов: %d</b>".formatted(context.entityIds().size()), null);


        //Карточки Проектов
        for (int i = 0; i < pageProjects.size(); i++) {
            ProjectDto project = pageProjects.get(i);
            // Расчет номера проекта для форматирования
            String projectText = formatProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);

            // Клавиатура: "Детали" / "Откликнуться"
            InlineKeyboardMarkup projectKeyboard = projectKeyboards.createProjectPreviewKeyboard(project.getId());

            Integer newMessageId = botExecutor.sendHtmlMessageReturnId(chatId, projectText, projectKeyboard);
            if (newMessageId != null) {
                messageIds.add(newMessageId);
            }
        }

        // Пагинация
        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);
        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);



        return messageIds;
    }

    // 🔥 Функция рендеринга: принимает данные и возвращает список ID отправленных сообщений
    public List<Integer> renderFavoritesPage(List<Long> pageProjectIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        // Получаем проекты по ID
        List<ProjectDto> pageProjects = projectService.getProjectsByIds(pageProjectIds);

// Заголовок
        String headerText = String.format("""
                ⭐ <b>ИЗБРАННЫЕ ПРОЕКТЫ</b>

                <i>Найдено %d проектов. Страница %d из %d</i>
                """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());

        Integer headerId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.editMessageWithHtml(chatId, headerId, headerText, null);

        // 2. Отправка Карточек
        for (int i = 0; i < pageProjects.size(); i++) {
            ProjectDto project = pageProjects.get(i);
            String projectCardText = formatProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);

            InlineKeyboardMarkup keyboard = projectKeyboards.createProjectPreviewKeyboard(project.getId());
            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, projectCardText, keyboard);
            if (cardId != null) messageIds.add(cardId);
        }

        // Пагинация

        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);
        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);

        return messageIds;
    }

    private String formatProjectPreview(ProjectDto project, int number) {
        return """
            🎯 <b>**Проект #%d**</b>

            <blockquote><b>💼 *%s*</b>
            <b>💰 Бюджет:</b> *%.0f руб*
            <b>⏱️ Срок:</b> *%d дней*
            <b>👀 Просмотров:</b> *%d*
            <b>📨 Откликов:</b> *%d*

            📝 <i>%s</i></blockquote>
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

    // 🔥 ФОРМАТИРОВАНИЕ КАРТОЧКИ ПРОЕКТА ДЛЯ ЗАКАЗЧИКА
    private String formatCustomerProjectPreview(ProjectDto project, int number) {
        return """
        🎯 <b>**Проект #%d**</b>

        <blockquote><b>💼 %s</b>
        <b>💰 Бюджет:</b> %.0f руб
        <b>⏱️ Срок:</b> %d дней
        <b>📊 Статус:</b> %s
        <b>👀 Просмотров:</b> %d
        <b>📨 Откликов:</b> %d</blockquote>
        """.formatted(
                number,
                project.getTitle(),
                project.getBudget(),
                project.getEstimatedDays(),
                getProjectStatusDisplay(project.getStatus()),
                project.getViewsCount(),
                project.getApplicationsCount()
        );
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
}
