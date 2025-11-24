package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class WithdrawApplicationCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationService applicationService;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "withdraw".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
            Long applicationId = Long.parseLong(context.getParameter());

            applicationService.withdrawApplication(applicationId, chatId);

            String successText = """
                ↩️<b> **ОТКЛИК ОТОЗВАН** </b>
                
                📨<i> Заявка успешно отозвана
                👔 Заказчик уведомлен</i>
                """;

            InlineKeyboardMarkup keyboard = commonKeyboards.createToMainMenuKeyboard();

            botExecutor.editMessageWithHtml(chatId, messageId, successText, keyboard);


            log.info("✅ Пользователь {} отозвал отклик {}",chatId, applicationId);
        } catch (Exception e) {
            log.error("❌ Ошибка отзыва отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка отзыва отклика: " + e.getMessage(), 5);
        }
    }
}
