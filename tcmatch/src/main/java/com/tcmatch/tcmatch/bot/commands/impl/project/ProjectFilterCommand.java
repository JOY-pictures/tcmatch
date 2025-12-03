package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.SearchRequest;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.PROJECTS_PER_PAGE;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectFilterCommand implements Command {

    private final UserSessionService userSessionService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final PaginationManager paginationManager;
    private final ProjectService projectService;

    private static final String SEARCH_STATE_KEY = "search_request_data";

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "filter".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        String parameter = context.getParameter();

        // Получаем текущий DTO
        SearchRequest currentRequest = userSessionService.getFromContext(chatId, SEARCH_STATE_KEY, SearchRequest.class);
        if (currentRequest == null) {
            currentRequest = SearchRequest.empty();
        }

        // --- 1. НАЧАЛО ИЛИ ПЕРЕРИСОВКА ФОРМЫ ---
        if (parameter == null || parameter.isEmpty() ||"start".equals(parameter) || "clear".equals(parameter)) {

            handleFilterStart(context, currentRequest, parameter);
            return;

        }

        // --- 2. ПРИМЕНЕНИЕ ФИЛЬТРОВ И ПЕРЕХОД К ПАГИНАЦИИ ---
        if ("apply".equals(parameter)) {
            // Логика перехода к Режиму 2
            handleFilterApply(context, currentRequest);
            return;
        }

        if (parameter.startsWith("budget:")) {
            // 1. ИЗВЛЕКАЕМ ЗНАЧЕНИЕ БЮДЖЕТА
            // Наша строка: "budget:50000" или "budget:clear"
            handleBudgetFilter(context, parameter);
            return;
        }
    }

    private void handleFilterStart(CommandContext context, SearchRequest currentRequest, String parameter) {
        if ("clear".equals(parameter)) {
            currentRequest = SearchRequest.empty();
            userSessionService.putToContext(context.getChatId(), SEARCH_STATE_KEY, currentRequest);
        }

        showSearchForm(context.getChatId(), context.getMessageId(), currentRequest);
    }

    private void handleFilterApply(CommandContext context, SearchRequest searchRequest) {
        List<Long> searchResultIds = projectService.searchActiveProjectIds(searchRequest);

        if (searchResultIds.isEmpty()) {
            showNoResults(context);
            return;
        }

        userSessionService.putToContext(context.getChatId(), SEARCH_STATE_KEY, searchRequest);

        paginationManager.renderIdBasedPage(
                context.getChatId(),
                "project_search",
                searchResultIds,
                "PROJECT",
                "init",
                PROJECTS_PER_PAGE,
                this::renderSearchPage
        );
    }

    private void handleBudgetFilter(CommandContext context, String parameter) {
        String budgetValue = parameter.substring("budget:".length());
        SearchRequest currentRequest = userSessionService.getFromContext(context.getChatId(), SEARCH_STATE_KEY, SearchRequest.class);
        if (currentRequest == null) {
            currentRequest = SearchRequest.empty();
        }

        int newMinBudget;
        try {
            if ("clear".equals(budgetValue)) {
                newMinBudget = 0;
            } else {
                newMinBudget = Integer.parseInt(budgetValue);
            }
        } catch (NumberFormatException e) {
            log.error("❌ Некорректное значение бюджета: {}", budgetValue);
            return;
        }

        currentRequest.setMinBudget(newMinBudget > 0 ? newMinBudget : null);
        userSessionService.putToContext(context.getChatId(), SEARCH_STATE_KEY, currentRequest);

        showSearchForm(context.getChatId(), context.getMessageId(), currentRequest);


    }

    private void showSearchForm(Long chatId, Integer messageId, SearchRequest currentRequest) {
        String text = """
            🔍<b> **ПОИСК ПРОЕКТОВ TCMatch** </b>

            🚀 <i>*Выберите фильтр для начала поиска*</i>
            """;
        InlineKeyboardMarkup keyboard = projectKeyboards.createFilterSelectionKeyboard(currentRequest);
        botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);
    }

    private void showNoResults(CommandContext context) {
        String text = """
            🔍 <b>**ПРОЕКТЫ НЕ НАЙДЕНЫ**</b>

            💡<i> По вашему запросу ничего не нашлось.</i>
            """;
        InlineKeyboardMarkup keyboard = commonKeyboards.createOneButtonKeyboard("✏️ Изменить фильтр", "project:filter:start");
        botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), text, keyboard);
    }

    public List<Integer> renderSearchPage(List<Long> pageProjectIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);

        // Получаем проекты по ID
        List<ProjectDto> pageProjects = projectService.getProjectsByIds(pageProjectIds);

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

        botExecutor.editMessageWithHtml(chatId, mainMessageId, "<b>🔍Найдено проектов: %d</b>".formatted(context.entityIds().size()), null);

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
}
