package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.APPLICATIONS_PER_PAGE;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowProjectApplicationsCommand implements Command {

    private final UserService userService;
    private final BotExecutor botExecutor;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;    private final PaginationManager paginationManager;

    private static final String PROJECT_APPLICATIONS_CONTEXT_KEY = "PROJECT_APPLICATIONS_CONTEXT_KEY";

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "show_applications".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Long projectId = Long.parseLong(context.getParameter());

            // 🔥 ПОЛУЧАЕМ ИНФОРМАЦИЮ О ПРОЕКТЕ
            ProjectDto project = projectService.getProjectDtoById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            // 🔥 ПРОВЕРЯЕМ ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ПРОЕКТА
            if (!project.getCustomerChatId().equals(chatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Вы не являетесь владельцем этого проекта", 5);
                return;
            }

            // 🔥 1. Получаем ID всех откликов на проект
            List<Long> applicationIds = applicationService.getProjectApplicationIds(projectId);

            if (applicationIds.isEmpty()) {
                showNoApplicationsMessage(chatId, project);
                return;
            }

            // 🔥 2. Запускаем пагинацию
            paginationManager.renderIdBasedPage(
                    chatId,
                    PROJECT_APPLICATIONS_CONTEXT_KEY,
                    applicationIds,
                    "APPLICATION",
                    "init",
                    APPLICATIONS_PER_PAGE,
                    this::renderProjectApplicationsPage // 🔥 Передаем рендерер для заказчика
            );

            log.info("✅ Запущена пагинация откликов на проект {} для заказчика {}", projectId, chatId);

        } catch (Exception e) {
            log.error("❌ Ошибка показа откликов на проект: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка загрузки откликов: " + e.getMessage(), 5);
        }
    }

    // 🔥 РЕНДЕРЕР ДЛЯ ОТКЛИКОВ НА ПРОЕКТ (ДЛЯ ЗАКАЗЧИКА)
    private List<Integer> renderProjectApplicationsPage(List<Long> pageApplicationIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        // 🔥 ПОЛУЧАЕМ DTO ВМЕСТО ПОЛНЫХ СУЩНОСТЕЙ
        List<ApplicationDto> pageApplications = applicationService.getApplicationsByIds(pageApplicationIds);

        // Заголовок
        String headerText = String.format("""
            📨 <b>ОТКЛИКИ НА ПРОЕКТ</b>
            
            <i>Найдено %d откликов. Страница %d из %d</i>
            """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());

        Integer headerId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.editMessageWithHtml(chatId, headerId, headerText, null);

        // Карточки откликов (используем DTO)
        for (int i = 0; i < pageApplications.size(); i++) {
            ApplicationDto application = pageApplications.get(i);
            String applicationText = formatApplicationForCustomer(application, (context.currentPage() * context.pageSize()) + i + 1);
            InlineKeyboardMarkup keyboard = applicationKeyboards.createApplicationItemKeyboard(application.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, applicationText, keyboard);
            if (cardId != null) messageIds.add(cardId);
        }

        // Пагинация

        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);

        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);


        return messageIds;
    }

//    private Long extractProjectIdFromContext(PaginationContext context) {
//        try {
//            // 🔥 ИЗВЛЕКАЕМ PROJECT_ID ИЗ ACTION КОНТЕКСТА
//            String action = context.action();
//            if (action != null && action.startsWith("project_applications:")) {
//                return Long.parseLong(action.split(":")[1]);
//            }
//            throw new RuntimeException("Project ID not found in context");
//        } catch (Exception e) {
//            log.error("❌ Ошибка извлечения projectId из контекста: {}", e.getMessage());
//            throw new RuntimeException("Не удалось определить проект");
//        }
//    }

    private String createApplicationsHeader(ProjectDto project, PaginationContext context) {
        if (project == null) {
            return String.format("""
                📨 <b>ОТКЛИКИ НА ПРОЕКТ</b>
                
                <i>Найдено %d откликов. Страница %d из %d</i>
                """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());
        }

        return String.format("""
            📨 <b>ОТКЛИКИ НА ПРОЕКТ</b>
            
            <blockquote>🎯 <b>Проект:</b> %s
            💰 <b>Бюджет:</b> <code>%.0f руб</code>
            ⏱️ <b>Срок:</b> <code>%d дней</code></blockquote>
            
            <i>Найдено %d откликов. Страница %d из %d</i>
            """,
                escapeHtml(project.getTitle()),
                project.getBudget(),
                project.getEstimatedDays(),
                context.entityIds().size(),
                context.currentPage() + 1,
                context.getTotalPages()
        );
    }

    private String formatApplicationForCustomer(com.tcmatch.tcmatch.model.dto.ApplicationDto application, int number) {
        UserDto user = userService.getUserDtoByChatId(application.getFreelancerChatId()).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return """
            <b>📨 Отклик #%d</b>
            
            <blockquote><b>👨‍💻 Исполнитель:</b> %s
            <b>💰 Предложил:</b> %.0f руб
            <b>⏱️ Срок:</b> %d дней
            <b>📅 Отправлен:</b> %s
            <b>⭐ Рейтинг:</b> %.1f/5.0
            <b>📊 Статус:</b> %s
            
            <b>📝 Сообщение:</b>
            <i>%s</i></blockquote>
            """.formatted(
                number,
                user.getDisplayName() != null ?
                        "@" + user.getDisplayName() : "Пользователь",
                application.getProposedBudget(),
                application.getProposedDays(),
                application.getAppliedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                user.getProfessionalRating() != null ? user.getProfessionalRating() : 0.0,
                getApplicationStatusDisplay(application.getStatus()),
                application.getCoverLetter().length() > 200 ?
                        application.getCoverLetter().substring(0, 200) + "..." :
                        application.getCoverLetter()
        );
    }

    private String getApplicationStatusDisplay(UserRole.ApplicationStatus applicationStatus) {
        return switch (applicationStatus) {
            case PENDING -> "🟡 Ожидает рассмотрения";
            case ACCEPTED -> "✅ Принят";
            case REJECTED -> "❌ Отклонен";
            case WITHDRAWN -> "↩️ Отозван";
        };
    }

    private void showNoApplicationsMessage(Long chatId, ProjectDto project) {
        String message = """
            <b>📨 ОТКЛИКИ НА ПРОЕКТ</b>

            <blockquote>🎯 <b>Проект:</b> %s
            💰 <b>Бюджет:</b> <code>%.0f руб</code>
            ⏱️ <b>Срок:</b> <code>%d дней</code></blockquote>

            <b>💡 Пока нет откликов на ваш проект</b>

            <b>🚀 Советы для привлечения исполнителей:</b>
            • Уточните описание проекта
            • Укажите четкие требования  
            • Будьте активны в ответах
            • Отклики обычно появляются в течение 24 часов

            <b>📊 Статистика проекта:</b>
            • Просмотров: %d
            • Откликов: 0
            """.formatted(
                escapeHtml(project.getTitle()),
                project.getBudget(),
                project.getEstimatedDays(),
                project.getViewsCount() != null ? project.getViewsCount() : 0
        );

        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.editMessageWithHtml(chatId, mainMessageId, message,
                commonKeyboards.createBackButton());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}