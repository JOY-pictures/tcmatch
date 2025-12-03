package com.tcmatch.tcmatch.bot.commands.impl.registration;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class AcceptRulesCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "rules".equals(actionType) && "accept".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Optional<UserDto> userOpt = userService.getUserDtoByChatId(chatId);

            if (userOpt.isEmpty()) {
                botExecutor.sendTemporaryErrorMessage(chatId, "Пользователь не найден", 5);
                return;
            }

            UserDto userDto = userOpt.get();
            User user = userService.acceptRules(chatId);

            // Удаляем предыдущие сообщения
            botExecutor.deletePreviousMessages(chatId);

            // Очищаем экраны регистрации
            userSessionService.removeScreensOfType(chatId, "rules");
            userSessionService.removeScreensOfType(chatId, "register");

            String successText = """
                    <b>🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!</b>
                    
                    <i>🚀 Теперь вам доступен полный функционал платформы
                    
                    🏠 Можете переходить на главный экран</i>
                    """;

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            botExecutor.editMessageWithHtml(chatId, mainMessageId, successText,
                    commonKeyboards.createToMainMenuKeyboard());

            // Сбрасываем навигацию на главное меню
            userSessionService.pushToNavigationHistory(chatId, "main:menu");
            userSessionService.setCurrentCommand(chatId, "main");
            userSessionService.setCurrentAction(chatId, "main", "menu");

            log.info("🎉 User completed registration: {}", chatId);

        } catch (Exception e) {
            log.error("❌ Ошибка принятия правил: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка принятия правил", 5);
        }
    }
}
