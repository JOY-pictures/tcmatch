package com.tcmatch.tcmatch.bot.text.impl;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.text.TextCommand;
import com.tcmatch.tcmatch.model.UserSession;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminRejectionCommentTextCommand implements TextCommand {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final VerificationService verificationService;

    @Override
    public boolean canHandle(Long chatId, String text) {
        UserSession session = userSessionService.getSession(chatId);
        if (session == null) return false;

        // Проверяем, ждет ли админ ввода комментария для отклонения
        return session.getFromContext("awaiting_rejection_comment") != null;
    }

    @Override
    public void execute(Message message) {
        Long adminChatId = message.getChatId();
        String comment = message.getText();
        Integer messageId = message.getMessageId();

        UserSession session = userSessionService.getSession(adminChatId);
        if (session == null) return;

        // Получаем ID заявки из контекста
        Long requestId = (Long) session.getFromContext("awaiting_rejection_comment");
        Integer messageToDelete = (Integer) session.getFromContext("admin_pressed_message");

        if (requestId == null) {
            botExecutor.sendTemporaryErrorMessage(adminChatId, "❌ Ошибка: не найден ID заявки", 5);
            userSessionService.clearUserState(adminChatId);
            return;
        }

        try {
            // Удаляем сообщение с комментарием
            botExecutor.deleteMessage(adminChatId, messageId);

            botExecutor.deleteMessage(adminChatId, messageToDelete);

            // Отклоняем заявку с комментарием
            verificationService.rejectVerification(requestId, adminChatId, comment);

            // Очищаем состояние
            userSessionService.clearUserState(adminChatId);
            session.removeFromContext("awaiting_rejection_comment");
            session.removeFromContext("admin_pressed_message");

            // Отправляем подтверждение
            botExecutor.sendTemporaryErrorMessage(adminChatId,
                    "✅ Заявка #" + requestId + " отклонена",
                    3);



            log.info("Админ {} отклонил заявку #{} с комментарием", adminChatId, requestId);

        } catch (Exception e) {
            log.error("Ошибка отклонения заявки: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(adminChatId,
                    "❌ Ошибка отклонения: " + e.getMessage(), 5);

            // Очищаем состояние в любом случае
            userSessionService.clearUserState(adminChatId);
            if (session != null) {
                session.removeFromContext("awaiting_rejection_comment");
            }
        }
    }
}