package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.service.ProjectService;
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
public class ShowMyProjectsListCommand implements Command {

    private final ProjectService projectService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final PaginationManager paginationManager;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "my_list".equals(action);

    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        try {


            // 🔥 ПОЛУЧАЕМ ID ПРОЕКТОВ ЗАКАЗЧИКА
            List<Long> projectIds = projectService.getProjectIdsByCustomerChatId(chatId);

            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ (если были)
            botExecutor.deletePreviousMessages(chatId);

            // 🔥 ГЛАВНОЕ СООБЩЕНИЕ С КНОПКОЙ "СОЗДАТЬ ПРОЕКТ"
            String mainText = """
            👔 <b>**МОИ ПРОЕКТЫ**</b>

            💼 <i>Управление вашими проектами</i>
            """;

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            InlineKeyboardMarkup mainKeyboard = projectKeyboards.createCustomerProjectsMainKeyboard();
            if (projectIds.isEmpty()) {
                String text = """

                📭 <b>ПРОЕКТЫ НЕ НАЙДЕНЫ</b>

                💡<u> Создайте первый проект чтобы найти исполнителя</u>
                """;
                botExecutor.editMessageWithHtml(chatId, mainMessageId, mainText + text, mainKeyboard);
                return;
            }



            // 🔥 КЛАВИАТУРА ДЛЯ ГЛАВНОГО СООБЩЕНИЯ

            botExecutor.editMessageWithHtml(chatId, mainMessageId, mainText, mainKeyboard);

            // 🔥 ЗАПУСКАЕМ ПАГИНАЦИЮ ЧЕРЕЗ PAGINATION MANAGER
            paginationManager.renderIdBasedPage(
                    chatId,
                    "customer_projects",     // контекст для пагинации
                    projectIds,              // ID проектов
                    "PROJECT",               // тип сущности
                    "init",                  // направление
                    PROJECTS_PER_PAGE,       // размер страницы
                    this::renderCustomerProjectsPage  // рендерер
            );

        } catch (Exception e) {
            log.error("❌ Ошибка показа списка проектов: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки проектов", 5);
        }
    }

    // 🔥 РЕНДЕРЕР ДЛЯ СТРАНИЦЫ ПРОЕКТОВ ЗАКАЗЧИКА
    public List<Integer> renderCustomerProjectsPage(List<Long> pageProjectIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        // Получаем проекты по ID
        List<Project> pageProjects = projectService.findAllProjectsByIds(pageProjectIds);

        // Отправляем карточки проектов
        for (int i = 0; i < pageProjects.size(); i++) {
            Project project = pageProjects.get(i);
            String projectText = formatCustomerProjectPreview(project, (context.currentPage() * context.pageSize()) + i + 1);

            // Клавиатура для карточки проекта
            InlineKeyboardMarkup projectKeyboard = projectKeyboards.createProjectPreviewKeyboard(project.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, projectText, projectKeyboard);
            if (cardId != null) messageIds.add(cardId);
        }


        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);
        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);


        return messageIds;
    }

    // 🔥 ФОРМАТИРОВАНИЕ КАРТОЧКИ ПРОЕКТА ДЛЯ ЗАКАЗЧИКА
    private String formatCustomerProjectPreview(Project project, int number) {
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
