package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.APPLICATIONS_PER_PAGE;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationPaginationCommand implements Command {

    private final BotExecutor botExecutor;
    private final PaginationManager paginationManager;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;
    private final ApplicationService applicationService;
    private final ProjectService projectService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "pagination".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            // Формат: "next:my_applications:APPLICATION" или "next:project_applications:APPLICATION"
            String[] parts = context.getParameter().split(":");
            if (parts.length < 3) return;

            String direction = parts[0];
            String contextKey = parts[1];
            String entityType = parts[2];

            // 🔥 ОПРЕДЕЛЯЕМ РЕНДЕРЕР ДЛЯ КОНТЕКСТА
            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = null;

            if (PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY.equals(contextKey)) {
                renderer = this::renderFreelancerApplicationsPage;
            } else if (PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY.equals(contextKey)) {
                renderer = this::renderProjectApplicationsPage;
            }

            if (renderer == null) {
                log.error("❌ Renderer not found for application context: {}", contextKey);
                return;
            }

            // 🔥 ВЫЗЫВАЕМ PAGINATION MANAGER
            paginationManager.renderIdBasedPage(
                    context.getChatId(),
                    contextKey,
                    null, // ID уже в контексте
                    entityType,
                    direction,
                    APPLICATIONS_PER_PAGE,
                    renderer
            );

        } catch (Exception e) {
            log.error("❌ Ошибка пагинации откликов: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка переключения страницы", 5);
        }
    }

    public List<Integer> renderFreelancerApplicationsPage(List<Long> pageApplicationIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        List<ApplicationDto> pageApplications = applicationService.getApplicationsByIds(pageApplicationIds);

        // Заголовок
        String headerText = String.format("""
            📨 <b>МОИ ОТКЛИКИ</b>
            
            <i>Найдено %d откликов. Страница %d из %d</i>
            """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());



        Integer headerId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.editMessageWithHtml(chatId, headerId, headerText, null);
//        if (headerId != null) messageIds.add(headerId);

        for (int i = 0; i < pageApplications.size(); i++) {
            ApplicationDto application = pageApplications.get(i);
            String applicationCardText = formatApplicationPreview(application, (context.currentPage() * context.pageSize()) + i + 1);

            InlineKeyboardMarkup keyboard = applicationKeyboards.createApplicationItemKeyboard(application.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, applicationCardText, keyboard);
            if (cardId != null) messageIds.add(cardId);
        }

        // Пагинация

        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);

        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);


        return messageIds;
    }

    // 🔥 МЕТОД РЕНДЕРИНГА ДЛЯ ОТКЛИКОВ НА ПРОЕКТ (ИСПОЛЬЗУЕТ DTO)
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
//            InlineKeyboardMarkup keyboard = keyboardFactory.createApplicationResponseKeyboard(application.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, applicationText, null);
            if (cardId != null) messageIds.add(cardId);
        }

        // Пагинация

        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);

        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);


        return messageIds;
    }

    private String formatApplicationForCustomer(ApplicationDto application, int number) {
        return """
            <b>📨 Отклик #%d</b>
            
            <blockquote><b>👨‍💻 Исполнитель:</b> %s
            <b>💰 Предложил:</b> %.0f руб
            <b>⏱️ Срок:</b> %d дней
            <b>📅 Отправлен:</b> %s
            <b>⭐ Рейтинг:</b> %.1f/5.0
            
            <b>📝 Сообщение:</b>
            <i>%s</i></blockquote>
            """.formatted(
                number,
                application.getFreelancer().getUserName() != null ?
                        "@" + application.getFreelancer().getUserName() : "Пользователь",
                application.getProposedBudget(),
                application.getProposedDays(),
                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                application.getFreelancer().getRating(),
                application.getCoverLetter().length() > 200 ?
                        application.getCoverLetter().substring(0, 200) + "..." :
                        application.getCoverLetter()
        );
    }

    private String formatApplicationPreview(ApplicationDto application, int number) {
        String projectTitle = projectService.getProjectTitleById(application.getProjectId());

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
                projectTitle,
                application.getProposedBudget(),
                application.getProposedDays(),
                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                getApplicationStatusDisplay(application.getStatus()),
                application.getCoverLetter().length() > 150 ?
                        application.getCoverLetter().substring(0, 150) + "..." :
                        application.getCoverLetter()
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
}
