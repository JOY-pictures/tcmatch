package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RejectApplicationCommand implements Command {

    private final ApplicationService applicationService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "reject".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        try {
            Long applicationId = Long.parseLong(context.getParameter());

            // 1. 🔥 ВЫПОЛНЯЕМ БИЗНЕС-ЛОГИКУ
            // (Пока без причины, для простоты)
            applicationService.rejectApplication(applicationId, chatId, null);

            log.info("Отклик {} отклонен заказчиком {}", applicationId, chatId);

            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

            botExecutor.editMessageWithHtml(chatId, messageId,"✅ <b>Отклик отклонён!</b> \n\n<u>Исполнитель был проинформирован</u>", commonKeyboards.createToMainMenuKeyboard());

        } catch (Exception e) {
            // TODO: Добавить обработку, если заказчик не является владельцем
            log.error("❌ Ошибка отклонения отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка отклонения отклика", 5);
        }
    }
}