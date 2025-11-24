package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.*;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.APPLICATIONS_PER_PAGE;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationMenuCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final PaginationManager paginationManager;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "menu".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        UserRole userRole = userService.getUserRole(chatId);

        if (userRole == UserRole.FREELANCER) {
            // Показать список откликов, которые фрилансер отправил (Мои отклики)
            handleShowMyApplications(chatId, messageId);
        } else if (userRole == UserRole.CUSTOMER) {
            // Показать список откликов, которые заказчик получил (Отклики на мои проекты)
            handleShowProjectListApplications(chatId, messageId);
        } else {
            // Если роль не определена или не соответствует
            log.warn("❌ User {} tried to access application menu with unsupported role: {}", chatId, userRole);
            botExecutor.sendTemporaryErrorMessage(chatId, "Доступ к разделу 'Отклики' для вашей роли ограничен.", 5);
        }
    }

    // 🔥 2. ВХОД В СПИСОК "ОТКЛИКИ НА МОИ ПРОЕКТЫ" (ЗАКАЗЧИК)
    public void handleShowProjectListApplications(Long chatId, Integer messageId) {
        try {

            // 1. Получаем ID всех проектов заказчика
            List<Long> projectIds = projectService.getProjectIdsByCustomerChatId(chatId);

            if (projectIds.isEmpty()) {
                showNoApplicationsMessage(chatId, messageId, UserRole.CUSTOMER); // Нет проектов
                return;
            }

            // 2. Получаем ID всех откликов на эти проекты
            List<Long> applicationIds = applicationService.getApplicationsByProjectIds(projectIds)
                    .stream().map(Application::getId).toList();

            if (applicationIds.isEmpty()) {
                showNoApplicationsMessage(chatId, messageId, UserRole.CUSTOMER); // Есть проекты, но нет откликов
                return;
            }

            // 3. Запускаем пагинацию
            paginationManager.renderIdBasedPage(
                    chatId,
                    PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY,
                    applicationIds,
                    "APPLICATION",
                    "init",
                    APPLICATIONS_PER_PAGE,
                    this::renderProjectApplicationsPage // 🔥 Передаем рендерер заказчика
            );

        } catch (Exception e) {
            log.error("❌ Ошибка показа откликов на проекты заказчика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки откликов на ваши проекты", 5);
        }
    }

    public void handleShowMyApplications(Long chatId, Integer messageId) {
        try {

            // 1. Получаем ID всех откликов фрилансера
            List<Long> applicationIds = applicationService.getApplicationsByFreelancerChatId(chatId)
                    .stream().map(Application::getId).toList();

            if (applicationIds.isEmpty()) {
                showNoApplicationsMessage(chatId, messageId, UserRole.FREELANCER);
                return;
            }

            // 2. Запускаем пагинацию
            paginationManager.renderIdBasedPage(
                    chatId,
                    PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY,
                    applicationIds,
                    "APPLICATION",
                    "init",
                    APPLICATIONS_PER_PAGE,
                    this::renderFreelancerApplicationsPage // 🔥 Передаем рендерер фрилансера
            );

        } catch (Exception e) {
            log.error("❌ Ошибка показа откликов фрилансера: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки ваших откликов", 5);
        }
    }

    // 🔥 3. Отображение пустого списка (с учетом роли)
    private void showNoApplicationsMessage(Long chatId, Integer messageId, UserRole role) {
        String text;
        if (role == UserRole.FREELANCER) {
            text = """
                📨 <b>**МОИ ОТКЛИКИ**</b>
                
                📭<i> Вы еще не откликались на проекты</i>
                
                💡 *Как найти проекты:*
                • Используйте поиск проектов
                • Изучите требования заказчиков
                • Отправляйте качественные отклики
                """;
        } else if (role == UserRole.CUSTOMER) {
            text = """
                📭 <b>**ОТКЛИКОВ НЕТ**</b>
                
                💡 <i>На ваши проекты еще никто не откликнулся, либо у вас нет активных проектов.</i>
                """;
        } else {
            text = "📭 Ничего не найдено";
        }

        // Предполагаем, что createBackButton возвращает кнопку "Назад"
        botExecutor.editMessageWithHtml(chatId, messageId, text, commonKeyboards.createBackButton());
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
//        if (headerId != null) messageIds.add(headerId);

        // Карточки откликов (используем DTO)
        for (int i = 0; i < pageApplications.size(); i++) {
            ApplicationDto application = pageApplications.get(i);
            String applicationText = formatApplicationForCustomer(application, (context.currentPage() * context.pageSize()) + i + 1);
//            InlineKeyboardMarkup keyboard = keyboardFactory.createApplicationResponseKeyboard(application.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, applicationText, null);
            if (cardId != null) messageIds.add(cardId);
        }

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
