package com.tcmatch.tcmatch.bot.commands.impl.help;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowHelpRulesCommand implements Command {

    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final ResourceLoader resourceLoader;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "help".equals(actionType) && "rules".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            String offerText = "Правила платформы";
            InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());
            botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, offerText, keyboard);

            String oferPath = "classpath:static/TCMatch-ofer.pdf";
            Resource resource = resourceLoader.getResource(oferPath);

            Integer docMessageId = botExecutor.sendDocMessageReturnId(context.getChatId(), resource, "Договор-оферта.pdf");
            if (docMessageId != null) {
                userSessionService.addTemporaryMessageId(context.getChatId(), docMessageId);
            }

        } catch (Exception e) {
            log.error("❌ Error showing rules for user {}: {}", context.getChatId(), e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка при загрузке правил", 5);
        }
    }
}