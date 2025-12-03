package com.tcmatch.tcmatch.bot.commands.impl.common;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class MainMenuCommand implements Command {

    private final UserSessionService userSessionService;

    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final TextMessageService textMessageService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "main".equals(actionType) && "menu".equals(action);    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            String text = textMessageService.getMainMenuText();

            if (userSessionService.getUserHistory(chatId) != null) {
                System.out.println(userSessionService.getUserHistory(chatId));
                userSessionService.resetToMain(chatId);
            }




            InlineKeyboardMarkup keyboard = commonKeyboards.createMainMenuKeyboard(chatId);

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Error showing main menu for user {}: {}", context.getChatId(), e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка при открытии главного меню", 5);
        }
    }
}