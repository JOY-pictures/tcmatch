package com.tcmatch.tcmatch.bot.text.impl;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.text.TextCommand;
import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubUrlTextCommand implements TextCommand {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final VerificationService verificationService;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(Long chatId, String text) {
        return userSessionService.isWaitingForGitHub(chatId);
    }

    @Override
    public void execute(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        Integer messageId = message.getMessageId();

        try {
            // 1. 🔥 ПРОСТАЯ ВАЛИДАЦИЯ
            if (!isValidGitHubUrl(text)) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Неверный формат GitHub URL. Пример: https://github.com/username", 5);
                return;
            }

            // 2. 🔥 УДАЛЯЕМ СООБЩЕНИЕ ПОЛЬЗОВАТЕЛЯ
            botExecutor.deleteMessage(chatId, messageId);

            // 3. 🔥 СОЗДАЕМ ЗАЯВКУ НА ВЕРИФИКАЦИЮ
            // Теперь передаем chatId, а не userId
            VerificationRequest request = verificationService.createGitHubVerificationRequest(
                    chatId, // 🔥 userChatId
                    text.trim()
            );

            // 4. 🔥 ОЧИЩАЕМ СОСТОЯНИЕ
            userSessionService.clearUserState(chatId);

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);

            // 5. 🔥 УВЕДОМЛЯЕМ ПОЛЬЗОВАТЕЛЯ
            String userMessage = String.format("""
            <b>✅ Заявка отправлена!</b>
            
            <b>GitHub:</b> %s
            <b>ID заявки:</b> <code>#%d</code>
            
            <i>Заявка будет рассмотрена в течение 1-2 рабочих дней.</i>
            """, text, request.getId());

            botExecutor.editMessageWithHtml(chatId, mainMessageId, userMessage, commonKeyboards.createToMainMenuKeyboard());

            log.info("✅ Пользователь {} отправил заявку на верификацию GitHub: {}", chatId, text);

        } catch (IllegalArgumentException e) {
            // Неверный URL
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ " + e.getMessage(), 5);

        } catch (IllegalStateException e) {
            // Уже есть активная заявка
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ " + e.getMessage(), 5);
            userSessionService.clearUserState(chatId);

        } catch (Exception e) {
            log.error("❌ Ошибка обработки GitHub URL: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Ошибка отправки заявки", 5);
            userSessionService.clearUserState(chatId);
        }
    }

    /**
     * 🔥 ПРОСТАЯ ВАЛИДАЦИЯ GITHUB URL
     */
    private boolean isValidGitHubUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmed = url.trim();

        // Простая проверка
        return trimmed.startsWith("https://github.com/") &&
                trimmed.length() > "https://github.com/".length() &&
                !trimmed.contains(" ") &&
                trimmed.length() < 100;
    }
}