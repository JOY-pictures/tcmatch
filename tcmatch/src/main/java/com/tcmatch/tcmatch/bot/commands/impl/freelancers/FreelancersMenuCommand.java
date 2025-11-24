package com.tcmatch.tcmatch.bot.commands.impl.freelancers;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.FreelancersKeyboards;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class FreelancersMenuCommand implements Command {

    private final BotExecutor botExecutor;
    private final FreelancersKeyboards freelancersKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "freelancers".equals(actionType) && "menu".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            String text = """
                👥 <b>ПОИСК ИСПОЛНИТЕЛЕЙ</b>
                
                <i>Выберите действие:</i>
                """;

            InlineKeyboardMarkup keyboard = freelancersKeyboards.createFreelancersMenuKeyboard();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Error showing freelancers menu for user {}: {}", context.getChatId(), e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка при открытии раздела исполнителей", 5);
        }
    }
}
