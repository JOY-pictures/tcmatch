package com.tcmatch.tcmatch.bot.commands.impl.admin;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 🔥 Команда для отмены отклонения
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CancelRejectionCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "admin".equals(actionType) &&
                action.startsWith("verification:cancel_reject:");
    }

    @Override
    public void execute(CommandContext context) {
        Long adminChatId = context.getChatId();

        // Очищаем состояние ожидания комментария
        userSessionService.clearUserState(adminChatId);

//        // Уведомляем админа
//        botExecutor.sendTemporaryMessage(adminChatId,
//                "🚫 Отклонение отменено", 3, null);

        // Удаляем или редактируем сообщение с запросом комментария
        botExecutor.deleteMessage(adminChatId, context.getMessageId());
    }
}
