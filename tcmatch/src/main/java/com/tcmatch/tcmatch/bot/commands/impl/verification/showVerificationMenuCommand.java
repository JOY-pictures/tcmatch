package com.tcmatch.tcmatch.bot.commands.impl.verification;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.VerificationKeyboards;
import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@RequiredArgsConstructor
@Slf4j
public class showVerificationMenuCommand implements Command {

    private final VerificationKeyboards verificationKeyboards;
    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final UserService userService;
    private final VerificationService verificationService;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "verification".equals(actionType) && "show".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
        try {
            // 1. Проверяем, существует ли пользователь
            if (!userService.userExists(chatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Сначала завершите регистрацию", 5);
                return;
            }

            // 2. Проверяем статус верификации (теперь по chatId)
            VerificationStatus status = verificationService.getGitHubVerificationStatus(chatId);

            String text = formatVerificationStatus(status, chatId);
            InlineKeyboardMarkup keyboard = verificationKeyboards.createMenuKeyboard(status, chatId);

            botExecutor.editMessageWithHtml(chatId, mainMessageId, text, keyboard);
        } catch (Exception e) {
            log.error("Ошибка при старте верификации: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка: " + e.getMessage(), 5);
        }
    }

    private String formatVerificationStatus (VerificationStatus status, Long chatId) {
        String text = "<b>☑️Статус верификации</b>\n\n";
        if (status == null) {
            text += """
                    <i>Вы ещё не отправляли заявку на верификацию пользователя на платформе GitHub</i>
                    
                    Можете сделать это сейчас, нажав на кнопку снизу""";
        }
        else if (status == VerificationStatus.PENDING) {
            VerificationRequest request = verificationService.getCurrentGitHubVerificationRequest(chatId).orElseThrow(() -> new RuntimeException("заявка не найдена"));

            text += String.format("""
                    <b>Ваша заявка на рассмотрении</b>
                    
                    <b>🔗 GitHub:</b> <code>%s</code>
                    <b>📅 Дата проверки:</b> %s
                    <b>🔢 ID заявки:</b> <code>#%d</code>
                    
                    <i>Дождитесь модерации!</i>
                    """, request.getProvidedData(), request.getReviewedAt(), request.getId());
        }
        else if (status == VerificationStatus.APPROVED) {
            VerificationRequest request = verificationService.getCurrentGitHubVerificationRequest(chatId).orElseThrow(() -> new RuntimeException("заявка не найдена"));

            text += String.format("""
                    <b>Ваша заявка была принята!</b>
                    
                    <b>🔗 GitHub:</b> <code>%s</code>
                    <b>📅 Дата проверки:</b> %s
                    <b>🔢 ID заявки:</b> <code>#%d</code>
                    
                    <i>Ваш профиль отмечен как верифицированный</i>
                    """, request.getProvidedData(), request.getReviewedAt(), request.getId());
        }
        else if (status == VerificationStatus.REJECTED) {
            VerificationRequest request = verificationService.getCurrentGitHubVerificationRequest(chatId).orElseThrow(() -> new RuntimeException("заявка не найдена"));

            text += String.format("""
                    <b>Ваша заявка была отклонена!</b>
                    
                    <b>🔗 GitHub:</b> <code>%s</code>
                    <b>📅 Дата проверки:</b> %s
                    <b>🔢 ID заявки:</b> <code>#%d</code>
                    
                    Вы можете отправить заявку повторно через день после отклонения последнего
                    """, request.getProvidedData(), request.getReviewedAt(), request.getId());
        }
        return text;
    }
}
