package com.tcmatch.tcmatch.bot.commands.impl.admin;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.service.AdminService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminVerificationActionCommand implements Command {

    private final BotExecutor botExecutor;
    private final VerificationService verificationService;
    private final AdminService adminService;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "admin".equals(actionType) && action.equals("verification");
    }

    @Override
    public void execute(CommandContext context) {
        Long adminChatId = context.getChatId();
        Integer messageId = context.getMessageId();

        // Проверяем права админа
        if (!adminService.isAdmin(adminChatId)) {
            botExecutor.sendTemporaryErrorMessage(adminChatId, "⛔ У вас нет доступа", 5);
            return;
        }

        String[] parts = context.getParameter().split(":");

        try {
            if (parts.length >= 2) {
                Long requestId = Long.parseLong(parts[1]);

                if (context.getParameter().contains("approve")) {
                    approveVerification(adminChatId, requestId, messageId);
                } else if (context.getParameter().contains("reject")) {
                    askRejectionComment(adminChatId, requestId, messageId);
                }
            }

        } catch (NumberFormatException e) {
            botExecutor.sendTemporaryErrorMessage(adminChatId, "❌ Неверный ID заявки", 5);
        } catch (Exception e) {
            log.error("Ошибка действия админа: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(adminChatId, "❌ Ошибка: " + e.getMessage(), 5);
        }
    }

    /**
     * 🔥 ОДОБРЕНИЕ ЗАЯВКИ
     */
    private void approveVerification(Long adminChatId, Long requestId, Integer messageId) {
        try {
            // Одобряем заявку
            verificationService.approveVerification(requestId, adminChatId);

            // УДАЛЯЕМ СООБЩЕНИЕ С УВЕДОМЛЕНИЕМ
            botExecutor.deleteMessage(adminChatId, messageId);

            // Отправляем краткое подтверждение
            botExecutor.sendTemporaryErrorMessageWithHtml(adminChatId,
                    "✅ Заявка #" + requestId + " одобрена",
                    3);

            log.info("Админ {} одобрил заявку #{}", adminChatId, requestId);

        } catch (Exception e) {
            log.error("Ошибка одобрения заявки: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(adminChatId,
                    "❌ Ошибка: " + e.getMessage(), 5);
        }
    }

    /**
     * 🔥 ЗАПРОС КОММЕНТАРИЯ ДЛЯ ОТКЛОНЕНИЯ
     */
    private void askRejectionComment(Long adminChatId, Long requestId, Integer messageId) {
        try {
            Optional<VerificationRequest> requestOpt = verificationService.getVerificationRequestById(requestId);

            if (requestOpt.isEmpty()) {
                botExecutor.sendTemporaryErrorMessage(adminChatId, "❌ Заявка не найдена", 5);
                return;
            }

            VerificationRequest request = requestOpt.get();

            // Отправляем запрос комментария
            String message = String.format("""
                <b>📝 Введите причину отклонения</b>
                
                <b>Заявка:</b> #%d
                <b>Пользователь:</b> @%s
                <b>GitHub:</b> <code>%s</code>
                
                <i>Отправьте текст комментария.</i>
                <i>Пользователь увидит этот комментарий.</i>
                """,
                    request.getId(),
                    request.getUserName() != null ? request.getUserName() : "без username",
                    request.getProvidedData()
            );

            botExecutor.editMessageWithHtml(adminChatId, messageId, message, null);

            // Устанавливаем состояние ожидания комментария
            userSessionService.setAwaitingRejectionComment(adminChatId, requestId);
            userSessionService.putToContext(adminChatId, "admin_pressed_message", messageId);

        } catch (Exception e) {
            log.error("Ошибка запроса комментария: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(adminChatId, "❌ Ошибка: " + e.getMessage(), 5);
        }
    }
}