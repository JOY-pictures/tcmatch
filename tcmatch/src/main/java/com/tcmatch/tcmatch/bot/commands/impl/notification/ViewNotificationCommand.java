package com.tcmatch.tcmatch.bot.commands.impl.notification;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.dispatcher.CommandDispatcher;
import com.tcmatch.tcmatch.model.Notification;
import com.tcmatch.tcmatch.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor // Создаст конструктор для final-полей (Service и Executor)
public class ViewNotificationCommand implements Command {

    private final NotificationService notificationService;
    private final BotExecutor botExecutor;

    // 🔥 РАЗРЫВАЕМ ЦИКЛ: Field Injection + @Lazy + @Autowired (не final)
    @Lazy
    @Autowired
    private CommandDispatcher commandDispatcher;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "notification".equals(actionType) && "view".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = context.getMessageId();

        try {
            Long notificationId = Long.parseLong(context.getParameter());

            // 1. ПОМЕЧАЕМ УВЕДОМЛЕНИЕ КАК ПРОЧИТАННОЕ
            notificationService.markAsRead(notificationId);

            // 2. ПОЛУЧАЕМ КОЛБЭК ДЛЯ ПЕРЕНАПРАВЛЕНИЯ
            Notification notification = notificationService.findById(notificationId);
            String redirectCallbackData = notification.getCallbackData();

//            // 3. ОТВЕЧАЕМ НА КОЛБЭК (убираем "часики")
//            botExecutor.answerCallbackQuery(context.getCallbackQueryId(), "Переход...");

            // 4. 🔥 ПЕРЕНАПРАВЛЕНИЕ (редирект)
            // Пример redirectCallbackData: "application:details:456"
            commandDispatcher.handleCallback(
                    chatId,
                    redirectCallbackData,
                    messageId,
                    context.getUserName()
            );

        } catch (NumberFormatException e) {
            log.error("❌ Invalid notification ID format: {}", context.getParameter());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка: неверный ID уведомления.", 5);
        } catch (Exception e) {
            log.error("❌ Ошибка при просмотре уведомления: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Произошла ошибка при переходе.", 5);
        }
    }
}