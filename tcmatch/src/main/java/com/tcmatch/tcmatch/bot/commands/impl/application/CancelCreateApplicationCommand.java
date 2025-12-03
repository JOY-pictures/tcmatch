package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.ApplicationCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CancelCreateApplicationCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationCreationService applicationCreationService;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "cancel".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        applicationCreationService.cancelCreation(context.getChatId());

        String text = """
        ❌ <b>**СОЗДАНИЕ ОТКЛИКА ОТМЕНЕНО**</b>
        
        <i>💡 Вы можете вернуться к проекту и создать отклик позже</i>
        """;

        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());

        botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, commonKeyboards.createToMainMenuKeyboard());

        log.info("❌ Пользователь {} отменил создание отклика", context.getChatId());
    }
}
