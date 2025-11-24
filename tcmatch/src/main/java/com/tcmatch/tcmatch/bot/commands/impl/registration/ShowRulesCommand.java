package com.tcmatch.tcmatch.bot.commands.impl.registration;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.RegistrationKeyboard;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowRulesCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final UserService userService;
    private final ResourceLoader resourceLoader;
    private final CommonKeyboards commonKeyboards;
    private final RegistrationKeyboard registrationKeyboard;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "rules".equals(actionType) && "view".equals(action);
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
            userService.markRulesViewed(chatId);

            String oferPath = "classpath:static/TCMatch-ofer.pdf";
            Resource resource = resourceLoader.getResource(oferPath);

            String rulesText = """
                <b>⬇️ Прочитайте правила ⬇️</b>
                
                <i>✅ Нажатием кнопки «Принять правила» Пользователь подтверждает,
                что ознакомлен и согласен со всеми условиями настоящей Оферты.</i>
                """;

            InlineKeyboardMarkup keyboard = registrationKeyboard.createRegistrationInProgressKeyboard(
                    UserRole.RegistrationStatus.RULES_VIEWED
            );

            Integer docMessageId = botExecutor.sendDocMessageReturnId(chatId, resource, "Документ-оферта.pdf");
            if (docMessageId != null) {
                userSessionService.addTemporaryMessageId(chatId, docMessageId);
            }

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            botExecutor.editMessageWithHtml(chatId, mainMessageId, rulesText, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка показа правил: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка загрузки правил", 5);
        }
    }
}
