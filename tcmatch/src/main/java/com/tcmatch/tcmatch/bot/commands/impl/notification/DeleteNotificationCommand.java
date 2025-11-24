package com.tcmatch.tcmatch.bot.commands.impl.notification;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.dispatcher.CommandDispatcher;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeleteNotificationCommand implements Command {

    private final NotificationService notificationService;
    private final BotExecutor botExecutor;
    // 🔥 Нам нужен CommandDispatcher для редиректа на перерисовку страницы
    @Lazy
    @Autowired
    private CommandDispatcher commandDispatcher;


    @Override
    public boolean canHandle(String actionType, String action) {
        return "notification".equals(actionType) && "delete".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();

        try {
            Long notificationId = Long.parseLong(context.getParameter());

            // 1. Удаляем уведомление из БД
            notificationService.deleteNotification(notificationId);

            log.info("🗑️ Уведомление #{} удалено пользователем {}", notificationId, chatId);

//            // 2. Уведомляем пользователя
//            botExecutor.answerCallbackQuery(context.getCallbackQueryId(), "✅ Уведомление удалено!");

            // 3. 🔥 Перерисовываем текущую страницу Центра уведомлений.
            // Нам нужно определить текущую страницу пагинации.

            // Получаем контекст, чтобы узнать, на какой странице мы сейчас были.
            // Для этого мы используем команду пагинации, которая умеет это делать.

            // Команда ShowNotificationCenterCommand сохранила ID в NOTIFICATION_CENTER_CONTEXT_KEY
            String contextKey = PaginationContextKeys.NOTIFICATION_CENTER_CONTEXT_KEY;

            // Мы вызываем диспетчер для редиректа на команду пагинации,
            // которая перечитает ID, обновит текущую страницу и перерисует.

            // Редирект на команду "notification:pagination" с параметром "redraw"
            // (или "current", в зависимости от того, как ты настроил логику пагинации).
            // В твоем ApplicationPaginationCommand ты использовал direction, поэтому используем "current"

            String redrawCallback = "notification:main";

            // 🔥 РЕДИРЕКТ
            commandDispatcher.handleCallback(
                    chatId,
                    redrawCallback,
                    context.getMessageId(),
                    context.getUserName()
            );

        } catch (NumberFormatException e) {
            log.error("❌ Invalid notification ID format: {}", context.getParameter());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка: неверный ID уведомления.", 5);
        } catch (Exception e) {
            log.error("❌ Ошибка при удалении уведомления: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Произошла ошибка при удалении.", 5);
        }
    }
}