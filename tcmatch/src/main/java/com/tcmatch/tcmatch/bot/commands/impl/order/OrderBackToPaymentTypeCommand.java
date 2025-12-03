package com.tcmatch.tcmatch.bot.commands.impl.order;

import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderBackToPaymentTypeCommand implements Command {

    // 🔥 Инжектируем команду, на которую нужно перенаправить
    private final OrderWizardStartCommand orderWizardStartCommand;

    // -------------------------------------------------------------------
    // МЕТОДЫ ИНТЕРФЕЙСА COMMAND
    // -------------------------------------------------------------------

    @Override
    public boolean canHandle(String actionType, String action) {
        // actionType = order, action = back_to_type
        return "order".equals(actionType) && "back_to_type".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();

//        // ВАЖНО: Мы полагаемся на то, что canHandle уже проверил actionType и action.
//        // Дополнительная проверка на то, что это CallbackQuery, все еще полезна.
//        if (!context.isCallbackQuery()) {
//            log.error("❌ OrderBackToPaymentTypeCommand должен вызываться только через CallbackQuery. ChatId: {}", chatId);
//            return;
//        }

        log.info("↩️ User {} chose to go back to payment type selection (order:back_to_type).", chatId);

        try {
            // 🔥 ПЕРЕНАПРАВЛЕНИЕ: Вызываем execute() нужной команды
            // context содержит sentMessageId, которое OrderSetPaymentTypeCommand
            // использует для редактирования сообщения и вывода Шага 1.
            orderWizardStartCommand.execute(context);

        } catch (Exception e) {
            log.error("❌ Ошибка при перенаправлении на OrderSetPaymentTypeCommand для {}: {}", chatId, e.getMessage(), e);
            // Отправка сообщения об ошибке
        }
    }
}