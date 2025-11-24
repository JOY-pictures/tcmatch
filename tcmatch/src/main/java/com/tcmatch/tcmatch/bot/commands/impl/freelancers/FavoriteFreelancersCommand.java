package com.tcmatch.tcmatch.bot.commands.impl.freelancers;

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
public class FavoriteFreelancersCommand implements Command {

    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "freelancers".equals(actionType) && "favorites".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            String text = """
                <b>⭐ ИЗБРАННЫЕ ИСПОЛНИТЕЛИ</b>
                
                <i>🚧 Раздел в разработке
                
                Здесь будут ваши сохраненные исполнители</i>
                """;

            InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Error showing favorite freelancers for user {}: {}", context.getChatId(), e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка при загрузке избранных исполнителей", 5);
        }
    }
}