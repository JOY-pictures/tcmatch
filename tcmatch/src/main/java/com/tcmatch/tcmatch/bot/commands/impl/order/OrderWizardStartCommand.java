package com.tcmatch.tcmatch.bot.commands.impl.order;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.OrderKeyboards;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderWizardStartCommand implements Command {

    private final ApplicationService applicationService;
    private final BotExecutor botExecutor;
    private final OrderKeyboards orderKeyboards;
    private final ProjectService projectService;
    private final UserSessionService userSessionService;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "order".equals(actionType) && "wizard_start".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        try {
            // 1. 🔥 ИЗВЛЕКАЕМ ID ОТКЛИКА ИЗ КОНТЕКСТА
            Long applicationId;

            if (context.getParameter() != null) {
                applicationId = Long.parseLong(context.getParameter());
            } else {
                applicationId = userSessionService.getOrderCreationState(chatId).getApplicationId();
            }

            ProjectDto project = projectService.getProjectDtoById(applicationService.getProjectIdByApplicationId(applicationId))
                    .orElseThrow(() -> new RuntimeException("Проект не найден!"));

            // 2. 🔥 ПОЛУЧАЕМ ДАННЫЕ ДЛЯ ПРОВЕРКИ И СОСТОЯНИЯ
            ApplicationDto application = applicationService.getApplicationDtoById(applicationId);

            // Проверка, что заказчик - владелец проекта (базовая проверка)
            if (!chatId.equals(project.getCustomerChatId())) {
                botExecutor.editMessageWithHtml(chatId, messageId, "❌ Вы не являетесь владельцем этого проекта.", null);
                return;
            }

            // 3. 🔥 ИНИЦИАЛИЗИРУЕМ СОСТОЯНИЕ МАСТЕРА
            OrderCreationState state = new OrderCreationState(
                    chatId,
                    applicationId,
                    application.getProjectId()
            );

            userSessionService.setOrderCreationState(chatId, state);
            userSessionService.setCurrentCommand(chatId, "order");
            userSessionService.setCurrentAction(chatId, "order", "creating");

            // 4. 🔥 ОТОБРАЖЕНИЕ ПЕРВОГО ШАГА: Выбор типа оплаты
            String projectTitle = projectService.getProjectTitleById(application.getProjectId());

            String message = String.format("""
                <b>📝 МАСТЕР СОЗДАНИЯ ЗАКАЗА</b>
                
                Вы собираетесь принять отклик от фрилансера @%s
                на проект: <b>%s</b>
                
                <b>Бюджет:</b> <code>%.0f руб</code> | <b>Срок:</b> <code>%d дней</code>
                
                <b>ШАГ 1/2: Выберите СХЕМУ ОПЛАТЫ.</b>
                """,
                    application.getFreelancer().getDisplayName(),
                    projectTitle,
                    application.getProposedBudget(),
                    application.getProposedDays()
            );

            botExecutor.editMessageWithHtml(chatId, messageId, message, orderKeyboards.createPaymentTypeChoiceKeyboard());

            log.info("🚀 Запущен мастер создания заказа для chatId {} по отклику {}", chatId, applicationId);

        } catch (Exception e) {
            log.error("❌ Ошибка запуска мастера заказа для chatId {}: {}", chatId, e.getMessage());
            botExecutor.editMessageWithHtml(chatId, messageId, "❌ Ошибка запуска мастера заказа: " + e.getMessage(), commonKeyboards.createToMainMenuKeyboard());
        }
    }
}
