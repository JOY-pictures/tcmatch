package com.tcmatch.tcmatch.bot.commands.impl.order;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.PaymentType;
import com.tcmatch.tcmatch.service.OrderService;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfirmOrderCreationCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final OrderService orderService;
    private final UserService userService; // 🔥 Нужен для получения контактов
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        // actionType = order, action = confirm_creation
        return "order".equals(actionType) && "confirm_creation".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long customerChatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(customerChatId);

        try {
            OrderCreationState state = userSessionService.getOrderCreationState(customerChatId);

            if (state == null || state.getCurrentStep() != OrderCreationState.CreationStep.CONFIRMATION) {
                botExecutor.editMessageWithHtml(customerChatId, messageId, "❌ Сессия подтверждения заказа истекла. Начните снова.", null);
                return;
            }

            // 1. 🔥 СОЗДАЕМ ЗАКАЗ (Критический шаг)
            // Здесь order.createOrderFromState(state) также вызывает applicationService.acceptApplication()
            Order newOrder = orderService.createOrderFromState(state);

            // 2. 🔥 ОЧИЩАЕМ СЕССИЮ МАСТЕРА
            userSessionService.clearOrderCreationState(customerChatId);
            userSessionService.clearCurrentCommand(customerChatId);

            // 3. 🔥 ПОЛУЧАЕМ КОНТАКТЫ
            UserDto customer = userService.getUserDtoByChatId(customerChatId)
                    .orElseThrow(() -> new EntityNotFoundException("Заказчик не найден."));
            UserDto freelancer = userService.getUserDtoByChatId(newOrder.getFreelancerChatId())
                    .orElseThrow(() -> new EntityNotFoundException("Исполнитель не найден."));

            // 4. 🔥 ОТПРАВЛЯЕМ ФИНАЛЬНОЕ СООБЩЕНИЕ ЗАКАЗЧИКУ (С Контактами Исполнителя)
            String customerMessage = createCustomerSuccessMessage(newOrder, freelancer, customer);
            botExecutor.editMessageWithHtml(customerChatId, messageId, customerMessage, commonKeyboards.createToMainMenuKeyboard());

            log.info("✅ Заказ ID: {} успешно создан. Контакты отправлены заказчику {} и исполнителю {}.",
                    newOrder.getId(), customerChatId, newOrder.getFreelancerChatId());

        } catch (Exception e) {
            log.error("❌ Фатальная ошибка создания заказа для chatId {}: {}", customerChatId, e.getMessage());
            userSessionService.clearNavigationHistory(customerChatId);
            botExecutor.editMessageWithHtml(customerChatId, messageId, "❌ Произошла критическая ошибка при создании заказа. Начните сначала.", commonKeyboards.createToMainMenuKeyboard());
        }
    }

    // Вспомогательный метод: Сообщение для Заказчика (Финальная версия)
    private String createCustomerSuccessMessage(Order order, UserDto freelancer, UserDto customer) {
        return String.format("""
            <b>🎉 ЗАКАЗ УСПЕШНО СОЗДАН!</b>
            
            Ваш заказ №%d по проекту <code>%d</code> зафиксирован.
            
            <b>--- ДАЛЬНЕЙШИЕ ДЕЙСТВИЯ ---</b>
            
            1. <b>Оплатите первый этап</b> (или полную сумму) в разделе ваших заказов.
            2. Свяжитесь с исполнителем (@%s) для начала работы.
            
            <b>Сумма первого платежа:</b> <code>%.0f руб</code> (%s)
            
            <b>⚠️ ПРАВИЛО ПЛАТФОРМЫ:</b>
            
            <b>ОПЛАТА ПРОХОДИТ ТОЛЬКО ЧЕРЕЗ БИРЖУ!</b>
            
            <b>Оплатить этап можно в сообщении с заказом в списке ваших заказов!</b> 
            Только эти транзакции будут засчитаны в систему <b>репутации</b> исполнителя и будут учитываться для вашего рейтинга как заказчика. 
            
            <b>ЛЮБЫЕ ДРУГИЕ ПЕРЕВОДЫ (напрямую) НЕ БУДУТ УЧТЕНЫ БИРЖЕЙ!</b>
            
            <b>--- КОНТАКТЫ ИСПОЛНИТЕЛЯ ---</b>
            👤 Имя: <b>%s</b>
            📞 Telegram (ссылка): <a href=\"tg://user?id=%d\">@%s</a>
            
            """,
                order.getId(), // %d
                order.getProjectId(), // %d
                freelancer.getUserName() != null ? freelancer.getUserName() : "Имя скрыто", // %s
                calculateFirstPaymentAmount(order), // 🔥 НОВЫЙ АРГУМЕНТ: %.0f
                order.getPaymentType().getDisplayName().toLowerCase(), // 🔥 НОВЫЙ АРГУМЕНТ: %s
                freelancer.getFirstName(), // %s
                freelancer.getChatId(), // %d
                freelancer.getUserName() != null ? freelancer.getUserName() : "Имя скрыто" // %s
        );
    }

    private double calculateFirstPaymentAmount(Order order) {
        // 🔥 Убедитесь, что getTotalBudget() возвращает double, а не String.
        if (order.getPaymentType() == PaymentType.FULL) {
            return order.getTotalBudget();
        }

        // Если поэтапная
        return order.getTotalBudget() / order.getMilestoneCount();
    }
}