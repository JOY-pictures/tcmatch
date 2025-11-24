package com.tcmatch.tcmatch.bot.dispatcher;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.service.NavigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommandDispatcher {

    private final List<Command> commands;
    private final BotExecutor botExecutor;
    private final NavigationService navigationService;

    public void handleCallback(Long chatId, String callbackData, Integer messageId, String userName) {
        String[] parts = callbackData.split(":");
        String actionType = parts[0];
        String action = parts[1];
        // 🔥 ПРОСТО БЕРЕМ ВСЕ ОСТАВШИЕСЯ ЧАСТИ КАК ПАРАМЕТР
        String parameter = parts.length > 2 ?
                String.join(":", Arrays.copyOfRange(parts, 2, parts.length)) : null;

        // 🔥 СОХРАНЕНИЕ ИСТОРИИ НАВИГАЦИИ
        navigationService.saveToNavigationHistory(chatId, actionType, action, parameter);

        log.info("🔄 Command: {}:{}:{} (user: {}, chat: {})",
                actionType, action, parameter, userName, chatId);

        CommandContext context = new CommandContext(chatId, action, parameter, messageId, userName, actionType);

        // Ищем подходящую команду
        for (Command command : commands) {
            if (command.canHandle(actionType, action)) {
                log.info("✅ Executing: {}", command.getClass().getSimpleName());
                command.execute(context);
                return;
            }
        }

        // Если команда не найдена
        log.warn("❌ Command not found: {}:{}", actionType, action);
        botExecutor.sendTemporaryErrorMessage(chatId, "Команда не найдена", 5);
    }
}