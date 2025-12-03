package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectMenuCommand implements Command {

    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final BotExecutor botExecutor;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "menu".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
        String text = """
            💼 <b>**РАЗДЕЛ ПРОЕКТОВ TCMatch**</b>

            <i>Выберите нужный раздел:</i>
            """;

        InlineKeyboardMarkup keyboard = projectKeyboards.createProjectsMenuKeyboard(chatId);
        botExecutor.editMessageWithHtml(chatId, mainMessageId, text, keyboard);
    }
}
