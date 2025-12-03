package com.tcmatch.tcmatch.bot.commands.impl.verification;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartGitHubVerificationCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final UserService userService;
    private final VerificationService verificationService;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "verification".equals(actionType) && "start_github".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();

        try {
            // 1. Проверяем, существует ли пользователь
            if (!userService.userExists(chatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Сначала завершите регистрацию", 5);
                return;
            }

            // 2. Проверяем статус верификации (теперь по chatId)
            VerificationStatus status = verificationService.getGitHubVerificationStatus(chatId);

            if (status == VerificationStatus.PENDING) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "⏳ У вас уже есть заявка на рассмотрении", 5);
                return;
            }

            if (status == VerificationStatus.APPROVED) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "✅ Ваш GitHub уже верифицирован", 5);
                return;
            }

            // 3. Устанавливаем состояние ожидания
            userSessionService.setWaitingForGitHub(chatId);

            // 4. Показываем простое сообщение
            String message = """
            <b>🔗 Отправьте ссылку на ваш GitHub</b>
            
            <i>Просто отправьте ссылку в формате:</i>
            <code>https://github.com/ваш_username</code>
            
            <b>Пример:</b>
            <code>https://github.com/ivanov</code>
            
            <b>⚠️Важно!</b>
            <b>В информацию профиля GitHub должно быть внесёно имя пользователя Telegram (UID), с которого вы отправляете данную заявку</b>
            
            <i>В противном случае заявка будет отклонена</i>
            
            После одобрения заявки можете убрать UID из профиля GitHub
            <b>Отправьте ссылку сейчас:</b>
            """;

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);

            botExecutor.editMessageWithHtml(chatId, mainMessageId, message, commonKeyboards.createToMainMenuKeyboard());
            log.info("Пользователь {} начал процесс верификации GitHub", chatId);

        } catch (Exception e) {
            log.error("Ошибка при старте верификации: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка: " + e.getMessage(), 5);
        }
    }
}