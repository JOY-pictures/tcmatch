package com.tcmatch.tcmatch.bot.commands.impl.help;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.HelpKeyboards;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class HelpMenuCommand implements Command {

    private final BotExecutor botExecutor;
    private final HelpKeyboards helpKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "help".equals(actionType) && "menu".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            String text = """
                ❓ <b>РАЗДЕЛ ПОМОЩИ</b>
                
                <i>Выберите нужный раздел:</i>
                """;

            InlineKeyboardMarkup keyboard = helpKeyboards.createHelpMenuKeyboard();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Error showing help menu for user {}: {}", context.getChatId(), e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка при открытии раздела помощи", 5);
        }
    }
}