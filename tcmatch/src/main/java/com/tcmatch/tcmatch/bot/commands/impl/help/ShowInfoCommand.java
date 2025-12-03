package com.tcmatch.tcmatch.bot.commands.impl.help;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowInfoCommand implements Command {

    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "help".equals(actionType) && "info".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            String text = """
                ℹ️ <b>ИНФОРМАЦИЯ О TCMATCH</b>
                
                <i>Информация о платформе...</i>
                """;

            InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Error showing info for user {}: {}", context.getChatId(), e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка при загрузке информации", 5);
        }
    }
}