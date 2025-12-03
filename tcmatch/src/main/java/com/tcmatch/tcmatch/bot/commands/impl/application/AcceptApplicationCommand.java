package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.commands.impl.order.OrderWizardStartCommand;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AcceptApplicationCommand implements Command {

    private final ApplicationService applicationService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final OrderWizardStartCommand orderWizardStartCommand;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "accept".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        try {
            Long applicationId = Long.parseLong(context.getParameter());

            // 1. 🔥 ВЫПОЛНЯЕМ БИЗНЕС-ЛОГИКУ
            // Этот метод также опубликует событие для "Наблюдателя"
//            applicationService.acceptApplication(applicationId, chatId);

            // 🔥 ПЕРЕНАПРАВЛЯЕМ НА МАСТЕР СОЗДАНИЯ ЗАКАЗА
            orderWizardStartCommand.execute(context);

        } catch (Exception e) {
            // TODO: Добавить обработку, если заказчик не является владельцем
            log.error("❌ Ошибка принятия отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка принятия отклика", 5);
        }
    }
}