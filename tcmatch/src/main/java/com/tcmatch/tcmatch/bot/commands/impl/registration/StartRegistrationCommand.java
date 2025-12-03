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
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartRegistrationCommand implements Command {

    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final RegistrationKeyboard registrationKeyboard;
    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "register".equals(actionType) && "start".equals(action);
    }

    @Override
    public void execute(CommandContext context) {

        UserDto userDto = userService.getUserDtoByChatId(context.getChatId()).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserRole.RegistrationStatus status = userService.getRegistrationStatus(userDto.getChatId());

        if (status == UserRole.RegistrationStatus.RULES_ACCEPTED) {
            // 🔥 РЕГИСТРАЦИЯ УЖЕ ЗАВЕРШЕНА
            String message = """
            ✅ <b>Регистрация уже завершена</b>
            
            Вы уже зарегистрированы в системе.
            """;
            InlineKeyboardMarkup keyboard = commonKeyboards.createMainMenuKeyboard(context.getChatId());
            botExecutor.editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), message, keyboard);
            return;
        }

        // 🔥 ПЕРЕДАЕМ ВСЕ ДАННЫЕ ПОЛЬЗОВАТЕЛЯ
        userService.registerFromTelegram(
                userDto.getChatId(),
                userDto.getUserName(),
                userDto.getFirstName(),
                userDto.getLastName()
        );

        // 🔥 ПОКАЗЫВАЕМ ВЫБОР РОЛИ
        String text = """
        🎯 <b>**ВЫБЕРИТЕ ВАШУ РОЛЬ**</b>
        
        <i>Как вы планируете использовать платформу?</i>
        
        👔 **ЗАКАЗЧИК** - размещаю проекты, ищу исполнителей
        👨‍💻 **ИСПОЛНИТЕЛЬ** - ищу проекты, выполняю заказы
        
        <u>💡 Вы сможете изменить роль позже в настройках</u>
        """;

        InlineKeyboardMarkup keyboard = registrationKeyboard.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.REGISTERED, context.getChatId());
        botExecutor.editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), text, keyboard);
        log.info("🚀 Registration started via callback for: {}", userDto.getChatId());
    }
}
