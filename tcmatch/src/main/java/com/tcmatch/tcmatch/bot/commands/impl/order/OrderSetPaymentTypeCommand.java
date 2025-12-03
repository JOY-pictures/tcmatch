package com.tcmatch.tcmatch.bot.commands.impl.order;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.OrderKeyboards;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.enums.PaymentType;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserSessionService;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSetPaymentTypeCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final OrderKeyboards orderKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        // actionType = order, action = set_type
        return "order".equals(actionType) && "set_type".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        try {
            OrderCreationState state = userSessionService.getOrderCreationState(chatId);

            if (state == null) {
                botExecutor.editMessageWithHtml(chatId, messageId, "❌ Сессия создания заказа истекла. Начните снова.", null);
                return;
            }

            // 1. 🔥 Получаем выбранный тип (FULL или MILESTONES)
            PaymentType paymentType = PaymentType.valueOf(context.getParameter());
            state.setPaymentType(paymentType);

            ApplicationDto application = applicationService.getApplicationDtoById(state.getApplicationId());
            String projectTitle = projectService.getProjectTitleById(state.getProjectId());

            String message;

            if (paymentType == PaymentType.MILESTONES) {
                // 2. Если MILESTONES, переходим на Шаг 2: Выбор количества этапов
                state.setCurrentStep(OrderCreationState.CreationStep.MILESTONE_COUNT_CHOICE);
                userSessionService.setOrderCreationState(chatId, state);

                message = String.format("""
                    <b>📝 МАСТЕР СОЗДАНИЯ ЗАКАЗА</b>
                    
                    Проект: <b>%s</b> | Бюджет, предлагаемый исполнителем: <code>%.0f руб</code>
                    
                    <b>ШАГ 2/2: Выберите количество этапов.</b>
                    (По умолчанию 1 этап = полная оплата)
                    """,
                        projectTitle,
                        application.getProposedBudget()
                );

                botExecutor.editMessageWithHtml(chatId, messageId, message, orderKeyboards.createMilestoneCountChoiceKeyboard(application.getProposedBudget()));

            } else {
                // 3. Если FULL, сразу переходим на Шаг 3: Подтверждение
                state.setCurrentStep(OrderCreationState.CreationStep.CONFIRMATION);
                state.setMilestoneCount(1); // 1 этап для полной оплаты
                userSessionService.setOrderCreationState(chatId, state);

                // Перенаправляем на команду подтверждения, чтобы не дублировать логику отображения
                // Мы ее еще не создали, но будем вызывать ее так:
                botExecutor.editMessageWithHtml(chatId, messageId, createConfirmationMessage(state, projectTitle, application), orderKeyboards.createConfirmationKeyboard());
            }

            log.info("Order wizard for chatId {} set payment type to {}", chatId, paymentType);

        } catch (Exception e) {
            log.error("❌ Ошибка установки типа оплаты: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка: " + e.getMessage(), 5);
        }
    }

    // Вспомогательный метод для отображения финального сообщения
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
