package com.tcmatch.tcmatch.bot.commands.impl.registration;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.RegistrationKeyboard;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SelectRoleCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final RegistrationKeyboard registrationKeyboard;


    @Override
    public boolean canHandle(String actionType, String action) {
        return "register".equals(actionType) && "role".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            String roleParam = context.getParameter();
            Optional<UserDto> userOpt = userService.getUserDtoByChatId(chatId);

            if (userOpt.isEmpty()) {
                botExecutor.sendTemporaryErrorMessage(chatId, "Пользователь не найден", 5);
                return;
            }

            UserDto userDto = userOpt.get();
            UserRole userRole = "customer".equals(roleParam) ? UserRole.CUSTOMER : UserRole.FREELANCER;

            User user = userService.updateUserRole(chatId, userRole);

            String text = """
                ✅ <b>РОЛЬ ВЫБРАНА</b>
                <i>%s</i>
                
                Уважаемый пользователь,
                
                <i>📋 Прежде чем начать использование нашей платформы, пожалуйста, ознакомьтесь внимательно с правилами пользования услугами.
                Вы можете сделать это прямо сейчас, нажав на кнопку ниже:</i>
                """.formatted(getRoleDisplay(userRole));

            InlineKeyboardMarkup keyboard = registrationKeyboard.createRegistrationInProgressKeyboard(
                    UserRole.RegistrationStatus.ROLE_SELECTED,
                    chatId
            );

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            botExecutor.editMessageWithHtml(chatId, mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка выбора роли: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка выбора роли", 5);
        }
    }

    private String getRoleDisplay(UserRole role) {
        return switch (role) {
            case FREELANCER -> "👨‍💻 Исполнитель";
            case CUSTOMER -> "👔 Заказчик";
            case ADMIN -> "⚡ Администратор";
            default -> "👤 Пользователь";
        };
    }
}
