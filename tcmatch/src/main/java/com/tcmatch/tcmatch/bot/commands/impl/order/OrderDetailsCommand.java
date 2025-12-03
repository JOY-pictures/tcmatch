package com.tcmatch.tcmatch.bot.commands.impl.order;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.OrderKeyboards;
import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.OrderStatus;
import com.tcmatch.tcmatch.service.OrderService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderDetailsCommand implements Command {

    private final BotExecutor botExecutor;
    private final OrderService orderService;
    private final UserService userService;
    private final ProjectService projectService;
    //    private final PaymentService paymentService; // 🔥 Для кнопки "Оплатить"
    private final OrderKeyboards orderKeyboards; // 🔥 Для кнопок ("Оплатить", "Завершить")
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        // Эта команда может вызываться либо напрямую (order:details)
        // Либо через перенаправление (тогда actionType/action могут быть от проекта/отклика)
        // Для простоты сделаем ее только для прямого вызова,
        // а в execute() будем использовать context.getParameter()
        return "order".equals(actionType) && "details".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.deletePreviousMessages(chatId);

        try {
            Long orderId = Long.parseLong(context.getParameter());
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Заказ не найден"));

            // 1. Определяем, кто смотрит
            boolean isCustomer = order.getCustomerChatId().equals(chatId);

            // 2. Получаем "контрагента" (вторую сторону)
            Long counterpartyChatId = isCustomer ? order.getFreelancerChatId() : order.getCustomerChatId();
            UserDto counterparty = userService.getUserDtoByChatId(counterpartyChatId)
                    .orElseThrow(() -> new RuntimeException("Контрагент не найден"));

            String projectTitle = projectService.getProjectTitleById(order.getProjectId());

            // 3. Форматируем сообщение
            String message = formatOrderDetails(order, counterparty, projectTitle, isCustomer);

            // 4. Генерируем клавиатуру
            InlineKeyboardMarkup keyboard = createOrderKeyboard(order, isCustomer);

            botExecutor.editMessageWithHtml(chatId, messageId, message, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка отображения деталей заказа: {}", e.getMessage());
            botExecutor.editMessageWithHtml(chatId, messageId, "❌ Ошибка отображения деталей заказа.", null);
        }
    }

    // 🔥 ГЕНЕРАЦИЯ КЛАВИАТУРЫ (ЗДЕСЬ БУДЕТ КНОПКА "ОПЛАТИТЬ")
    private InlineKeyboardMarkup createOrderKeyboard(Order order, boolean isCustomer) {
        if (isCustomer && order.getStatus() == OrderStatus.ACTIVE) {

            // (Логика расчета суммы этапа)
            double amountToPay = order.getTotalBudget() / order.getMilestoneCount();
            // (Логика получения URL оплаты)
//            String paymentUrl = paymentService.generatePaymentUrl(order, amountToPay, 1); // TODO: Нужна логика индекса этапа

            return orderKeyboards.createCustomerActiveOrderKeyboard(order.getId(), null);
        }

        if (!isCustomer && order.getStatus() == OrderStatus.ACTIVE) {
            // Кнопки для Исполнителя
            return orderKeyboards.createFreelancerActiveOrderKeyboard(order.getId());
        }

        return commonKeyboards.createBackButton(); // Клавиатура по умолчанию
    }

    // 🔥 ФОРМАТИРОВАНИЕ СООБЩЕНИЯ (контакты, статус и т.д.)
    private String formatOrderDetails(Order order, UserDto counterparty, String projectTitle, boolean isCustomer) {
        String role = isCustomer ? "Исполнитель" : "Заказчик";

        String contacts = String.format("""
                        <b>--- Контакты (%s) ---</b>
                        👤 Имя: <b>%s</b>
                        📞 Telegram: <a href="tg://user?id=%d">@%s</a>
                        """,
                role,
                counterparty.getFirstName(),
                counterparty.getChatId(),
                counterparty.getUserName() != null ? counterparty.getUserName() : "Имя скрыто"
        );
        String text = String.format("""
                        <b>📋 ДЕТАЛИ ЗАКАЗА №%d</b>
                        (Статус: <b>%s</b>)
                        
                        <b>Проект:</b> %s
                        
                        <b>--- Условия ---</b>
                        💰 Бюджет: <code>%.0f руб</code>
                        💳 Схема: %s (%d эт.)
                        
                        %s
                        """,
                order.getId(),
                order.getStatus().getDisplayName(),
                projectTitle,
                order.getTotalBudget(),
                order.getPaymentType().getDisplayName(),
                order.getMilestoneCount(),
                contacts
        );

        if (isCustomer) {
            text += """
                    <b>⚠️ ПРАВИЛО РЕПУТАЦИИ:</b>
                        Оплата засчитывается только при нажатии кнопки "Оплатить" в этом меню!""";
        } else {
            text += """
                    <b>⚠️ ПРАВИЛО РЕПУТАЦИИ:</b>
                        Репутация высчитывается только при оплате заказа заказчиком в этом меню!""";
        }

        return text;
    }
}