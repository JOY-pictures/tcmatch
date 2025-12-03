package com.tcmatch.tcmatch.bot.commands.impl.profile;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProfileKeyboards;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EditProfileCommand implements Command {

    private final CommonKeyboards commonKeyboards;
    private final ProfileKeyboards profileKeyboards;
    private final BotExecutor botExecutor;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "user_profile".equals(actionType) && "edit".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        String editText = """
                ✏️ <b>**РЕДАКТИРОВАНИЕ ПРОФИЛЯ**</b>
                
                <i>🚧 Функция в разработке</i>
                
                Скоро вы сможете:
                • Изменить специализацию
                • Добавить описание и навыки
                • Настроить уведомления
                """;

        botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), editText, commonKeyboards.createBackButton());
    }
}