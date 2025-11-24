package com.tcmatch.tcmatch.bot.dispatcher;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.text.TextCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TextCommandDispatcher {

    private final List<TextCommand> textCommands;

    private final BotExecutor botExecutor;

    public void handleTextMessage(Message message) {

        Long chatId = message.getChatId();
        String text = message.getText();
        Integer messageId = message.getMessageId();

        // Ищем подходящую текстовую команду
        for (TextCommand command : textCommands) {
            if (command.canHandle(chatId, text)) {
                try {
                    command.execute(message);
                    return;
                } catch (Exception e) {
                    log.error("❌ Error executing text command for user {}: {}", chatId, e.getMessage());
                    botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка обработки ввода", 5);
                }
                return;
            }
        }

        // Если команда не найдена - игнорируем или показываем подсказку
        log.debug("No text command found for user {}: {}", chatId, text);
        botExecutor.deleteMessage(chatId, messageId);
        showUnknownCommandHint(chatId);
    }

    private void showUnknownCommandHint(Long chatId) {
        String hintText = """
            ⚠️ <b>Неизвестная команда</b>
            
            <i>Используйте кнопки меню для навигации</i>
            
            💡 <b>Доступные команды:</b>
            • /start - Перезапустить бота
            • Используйте кнопки для всех действий
            """;

        botExecutor.sendTemporaryErrorMessage(chatId, hintText, 10);
    }
}
