package com.tcmatch.tcmatch.bot.commands.impl.order;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.OrderKeyboards;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSetMilestoneCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final OrderKeyboards orderKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        // actionType = order, action = set_milestones
        return "order".equals(actionType) && "set_milestones".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        try {
            OrderCreationState state = userSessionService.getOrderCreationState(chatId);

            if (state == null || state.getPaymentType() == null) {
                botExecutor.editMessageWithHtml(chatId, messageId, "❌ Сессия создания заказа истекла или не выбран тип оплаты.", null);
                return;
            }

            // 1. 🔥 Получаем количество этапов (2 или 3)
            int milestoneCount = Integer.parseInt(context.getParameter());

            if (milestoneCount < 2 || milestoneCount > 3) {
                throw new IllegalArgumentException("Неверное количество этапов.");
            }

            state.setMilestoneCount(milestoneCount);
            state.setCurrentStep(OrderCreationState.CreationStep.CONFIRMATION);
            userSessionService.setOrderCreationState(chatId, state);

            // 2. 🔥 ГОТОВИМСЯ К ОТОБРАЖЕНИЮ ФИНАЛЬНОГО ПОДТВЕРЖДЕНИЯ

            ApplicationDto application = applicationService.getApplicationDtoById(state.getApplicationId());

            String projectTitle = projectService.getProjectTitleById(state.getProjectId());

            // 3. 🔥 ВЫВОД ФИНАЛЬНОГО СООБЩЕНИЯ
            String message = createConfirmationMessage(state, projectTitle, application);

            botExecutor.editMessageWithHtml(chatId, messageId, message, orderKeyboards.createConfirmationKeyboard());

            log.info("Order wizard for chatId {} set milestone count to {}", chatId, milestoneCount);

        } catch (Exception e) {
            log.error("❌ Ошибка установки количества этапов: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка: " + e.getMessage(), 5);
        }
    }

    // Вспомогательный метод для отображения финального сообщения (дублирует логику из OrderSetPaymentTypeCommand,
    // но это нормально для читаемости команд)
    private String createConfirmationMessage(OrderCreationState state, String projectTitle, ApplicationDto application) {
        return String.format("""
            <b>📝 МАСТЕР СОЗДАНИЯ ЗАКАЗА (ФИНАЛ)</b>
            
            Проект: <b>%s</b>
            Исполнитель: @%s
            
            <b>--- Ваша Договоренность ---</b>
            💰 Бюджет: <code>%.0f руб</code>
            ⏱️ Срок: <code>%d дней</code>
            
            <b>Схема оплаты:</b> %s
            <b>Этапов:</b> %d
            
            <b>⚠️ ВАЖНОЕ ПРЕДУПРЕЖДЕНИЕ:</b>
            
            После создания заказа вы получите контактные данные исполнителя (исполнитель получит ваши). 
            
            <b>ВСЕ ПЛАТЕЖИ ВЫ БУДЕТЕ ОСУЩЕСТВЛЯТЬ НАПРЯМУЮ!</b> 
            
            Пожалуйста, будьте бдительны и убедитесь в честности партнера, прежде чем переводить средства. 
            Администрация не несет ответственности за прямые переводы.
            
            <b>Нажмите "Подтвердить", чтобы зафиксировать заказ и получить контакты.</b>
            """,
                projectTitle,
                application.getFreelancer().getDisplayName(),
                application.getProposedBudget(),
                application.getProposedDays(),
                state.getPaymentType().getDisplayName(),
                state.getMilestoneCount()
        );
    }
}