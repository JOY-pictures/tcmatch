package com.tcmatch.tcmatch.bot.commands.impl.purchase;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.dispatcher.CommandDispatcher;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.PurchaseConfirmationDto;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CancelPurchaseCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final CommonKeyboards commonKeyboards;

    @Lazy
    @Autowired
    private CommandDispatcher commandDispatcher; // 🔥 Инжектим диспетчер

    @Override
    public boolean canHandle(String actionType, String action) {
        return "purchase".equals(actionType) && "cancel".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = context.getMessageId();
        String parameter = context.getParameter();

        try {
            // Формат: targetId:cancelCallback
            String[] parts = parameter.split(":", 2);
            String targetId = parts[0];
            String cancelCallback = parts.length > 1 ? parts[1] : null;

            // Получаем подтверждение из сессии
            PurchaseConfirmationDto confirmationDto = userSessionService.getPurchaseConfirmation(chatId);

            if (confirmationDto != null && confirmationDto.getTargetId().equals(targetId)) {
                // 🔥 ЕСЛИ ЕСТЬ КОЛБЭК - ВЫЗЫВАЕМ ЕГО
                if (cancelCallback != null && !cancelCallback.isEmpty()) {
                    executeCancelCallback(chatId, cancelCallback, messageId);
                } else {
                    showDefaultCancelMessage(chatId, messageId);
                }
            } else {
                showDefaultCancelMessage(chatId, messageId);
            }

            // Очищаем подтверждение
            userSessionService.clearPurchaseConfirmation(chatId);

            log.info("Покупка отменена: chatId={}, targetId={}", chatId, targetId);

        } catch (Exception e) {
            log.error("Ошибка отмены покупки: {}", e.getMessage(), e);
            showDefaultCancelMessage(chatId, messageId);
            userSessionService.clearPurchaseConfirmation(chatId);
        }
    }

    /**
     * 🔥 Выполняет колбэк отмены через CommandDispatcher
     */
    private void executeCancelCallback(Long chatId, String cancelCallback, Integer messageId) {
        try {
            log.info("Выполняем cancelCallback через dispatcher: {}", cancelCallback);

            String userName = null;
            commandDispatcher.handleCallback(chatId, cancelCallback, messageId, userName);

        } catch (Exception e) {
            log.error("Ошибка выполнения cancelCallback: {}", e.getMessage(), e);
            showDefaultCancelMessage(chatId, messageId);
        }
    }

    /**
     * Показывает стандартное сообщение об отмене
     */
    private void showDefaultCancelMessage(Long chatId, Integer messageId) {
        String message = """
            ❌ <b>Покупка отменена</b>
            
            Операция была отменена.
            Средства не списаны.
            """;

        botExecutor.editMessageWithHtml(chatId, botExecutor.getOrCreateMainMessageId(chatId), message, commonKeyboards.createToMainMenuKeyboard());
    }
}