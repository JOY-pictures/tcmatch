package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.APPLICATIONS_PER_PAGE;

@Component
@Slf4j
@RequiredArgsConstructor
public class MyAcceptedApplicationsCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationService applicationService;
    private final PaginationManager paginationManager; // 🔥 Добавлена инъекция менеджера
    private final ApplicationKeyboards applicationKeyboards; // 🔥 Предполагаем, что клавиатуры здесь
    private final CommonKeyboards commonKeyboards;
    private final ApplicationPaginationCommand applicationPaginationCommand;

    private static final int PAGE_SIZE = 5;

    @Override
    public boolean canHandle(String actionType, String action) {
        // actionType = application, action = accepted
        return "application".equals(actionType) && "accepted".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        // ID сообщения, которое нужно редактировать, берем из контекста колбэка
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        try {
            // 1. Получаем ID всех откликов фрилансера со статусом ACCEPTED
            List<Long> applicationIds = applicationService.getApplicationsByFreelancerChatIdAndStatus(
                            chatId,
                            UserRole.ApplicationStatus.ACCEPTED
                    )
                    .stream().map(Application::getId).toList();

            if (applicationIds.isEmpty()) {
                showNoAcceptedApplicationsMessage(chatId, messageId);
                return;
            }

            // 2. Инициализируем пагинацию, передавая рендерер из другого класса
            paginationManager.renderIdBasedPage(
                    chatId,
                    PaginationContextKeys.ACCEPTED_APPLICATIONS_CONTEXT_KEY,
                    applicationIds,
                    "APPLICATION",
                    "init",
                    APPLICATIONS_PER_PAGE,
                    // 🔥 ПЕРЕДАЧА РЕНДЕРЕРА ИЗ ДРУГОГО КЛАССА
                    applicationPaginationCommand::renderAcceptedApplicationsPage
            );

        } catch (Exception e) {
            log.error("❌ Ошибка инициализации списка выполняемых заказов: {}", e.getMessage(), e);
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки выполняемых заказов", 5);
        }
    }

    // Вспомогательный метод для случая, когда нет принятых откликов
    private void showNoAcceptedApplicationsMessage(Long chatId, Integer messageId) {
        String text = "⚙️ <b>У вас пока нет выполняемых заказов</b>\n<i>(принятых откликов).</i>\n" +
                "<i>Начните откликаться на проекты, чтобы зарабатывать!</i>";

        // Получаем клавиатуру "Назад в меню" или другую, если нужна
        InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();

        botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);
    }

//    // 🔥 РЕНДЕРЕР: Отображение страницы принятых откликов (выполняемых заказов)
//    private void renderAcceptedApplicationsPage(Long chatId, List<Application> applications, int currentPage, int totalPages, String paginationKey) {
//        String header = String.format("⚙️ <b>ВАШИ ВЫПОЛНЯЕМЫЕ ЗАКАЗЫ</b>\nСтраница %d из %d\n\n", currentPage, totalPages);
//
//        StringBuilder content = new StringBuilder();
//        for (Application app : applications) {
//            content.append(String.format(
//                    "• №%d | Проект: %s | Бюджет: <code>%.0f руб</code>\n",
//                    app.getId(),
//                    app.getProjectTitle(),
//                    app.getProposedBudget()
//            ));
//        }
//
//        // Создаем клавиатуру со ссылкой на application:details:ID и пагинацией
//        InlineKeyboardMarkup keyboard = applicationKeyboards.createAcceptedApplicationsListKeyboard(
//                applications,
//                currentPage,
//                totalPages,
//                paginationKey,
//                "application:accepted" // baseAction для пагинации
//        );
//
//        botExecutor.editMessageWithHtml(
//                chatId,
//                botExecutor.getOrCreateMainMessageId(chatId),
//                header + content.toString(),
//                keyboard
//        );
//    }
}