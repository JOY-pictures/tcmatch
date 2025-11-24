package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
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
public class FavoritesProjectsCommand implements Command {

    private final ProjectService projectService;
    private final PaginationManager paginationManager;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "favorites".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        try {

            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

            // 🔥 ПОЛУЧАЕМ ТОЛЬКО ID
            List<Long> favoriteIds = projectService.getFavoriteProjectIds(chatId);


            if (favoriteIds.isEmpty()) {
                String text = """
                        ⭐ <b>**ИЗБРАННЫЕ ПРОЕКТЫ**</b>

                        📭 <i>У вас пока нет избранных проектов</i>

                        💡<u> *Как добавить в избранное:*</u>
                        • <i>Находите интересный проект в поиске
                        • Нажимайте кнопку "⭐ В избранное"
                        • Возвращайтесь к нему позже</i>
                        """;
                botExecutor.editMessageWithHtml(chatId, messageId, text, commonKeyboards.createBackButton());
                return;
            }

            paginationManager.renderIdBasedPage(
                    chatId,
                    "favorites",           // контекст
                    favoriteIds,           // ID проектов
                    "PROJECT",             // тип сущности
                    "init",
                    PROJECTS_PER_PAGE,
                    this::renderFavoritesPage  // рендерер
            );
        } catch (Exception e) {
            log.error("❌ Ошибка показа избранных: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки избранных", 5);
        }
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


        botExecutor.editMessageWithHtml(chatId, userSessionService.getMainMessageId(chatId), headerText, null);

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
}
