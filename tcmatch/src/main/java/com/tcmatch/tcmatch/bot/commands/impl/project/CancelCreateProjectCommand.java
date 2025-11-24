package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.service.ProjectCreationService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CancelCreateProjectCommand implements Command {

    private final ProjectCreationService projectCreationService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "cancel_creation".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        projectCreationService.cancelCreation(context.getChatId());

        String text = """
        ❌ <b>**СОЗДАНИЕ ОТКЛИКА ОТМЕНЕНО**</b>
        
        <i>💡 Вы можете вернуться к проекту и создать отклик позже</i>
        """;

        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(context.getChatId());

        botExecutor.editMessageWithHtml(context.getChatId(), mainMessageId, text, commonKeyboards.createToMainMenuKeyboard());

        log.info("❌ Пользователь {} отменил создание отклика", context.getChatId());
    }
}
