package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Component
@Slf4j
public class RegistrationHandler extends BaseHandler {
    private final UserService userService;

    public RegistrationHandler(KeyboardFactory keyboardFactory, UserSessionService userSessionService, UserService userService) {
        super(keyboardFactory, userSessionService);
        this.userService = userService;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "register".equals(actionType) || "rules".equals(actionType);
    }

    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        // 🔥 СТАРАЯ ВЕРСИЯ - для обратной совместимости
        UserDto userDto = new UserDto(chatId, userName, null, null, messageId);
        handleWithUserDto(action, parameter, userDto);
    }

    // 🔥 НОВЫЙ МЕТОД С USER DTO
    public void handleWithUserDto(String action, String parameter, UserDto userDto) {
        log.debug("📝 Handling registration for user: {}", userDto.getDisplayName());

        switch (action) {
            case "start":
                startRegistration(userDto);
                break;
            case "view":
                showFullRules(userDto);
                break;
            case "accept":
                acceptRules(userDto);
                break;
            case "role":
                handleRoleSelection(userDto, parameter);
                break;
            default:
                log.warn("❌ Unknown registration action: {}", action);
        }
    }


    // 🔥 ОБНОВЛЯЕМ МЕТОДЫ С USER DTO
    private void startRegistration(UserDto userDto) {
        if (userService.userExists(userDto.getChatId())) {
            UserRole.RegistrationStatus status = userService.getRegistrationStatus(userDto.getChatId());
            String message = getRegistrationStatusMessage(status);
            InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(status);
            editMessage(userDto.getChatId(), userDto.getMessageId(), message, keyboard);
            return;
        }

        // 🔥 ПЕРЕДАЕМ ВСЕ ДАННЫЕ ПОЛЬЗОВАТЕЛЯ
        User user = userService.registerFromTelegram(
                userDto.getChatId(),
                userDto.getUserName(),
                userDto.getFirstName(),
                userDto.getLastName()
        );

        showRoleSelection(userDto);

        // 🔥 ПОКАЗЫВАЕМ ВЫБОР РОЛИ
        String roleSelectionText = """
        🎯 **ВЫБЕРИТЕ ВАШУ РОЛЬ**
        
        Как вы планируете использовать платформу?
        
        👔 **ЗАКАЗЧИК** - размещаю проекты, ищу исполнителей
        👨‍💻 **ИСПОЛНИТЕЛЬ** - ищу проекты, выполняю заказы
        
        💡 Вы сможете изменить роль позже в настройках
        """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.REGISTERED);
        editMessage(userDto.getChatId(), userDto.getMessageId(), roleSelectionText, keyboard);
        log.info("🚀 Registration started via callback for: {}", userDto.getChatId());
    }

    private void showRoleSelection(UserDto userDto) {
        String text = """
        🎯 **ВЫБЕРИТЕ ВАШУ РОЛЬ**
        
        Как вы планируете использовать платформу?
        
        👔 **ЗАКАЗЧИК** - размещаю проекты, ищу исполнителей
        👨‍💻 **ИСПОЛНИТЕЛЬ** - ищу проекты, выполняю заказы
        
        💡 Вы сможете изменить роль позже в настройках
        """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createRoleSelectionKeyboard();
        editMessage(userDto.getChatId(), userDto.getMessageId(), text, keyboard);
    }


    // 🔥 ОБРАБОТКА ВЫБОРА РОЛИ
    private void handleRoleSelection(UserDto userDto, String role) {
        UserRole userRole = "customer".equals(role) ? UserRole.CUSTOMER : UserRole.FREELANCER;

        User user = userService.updateUserRole(userDto.getChatId(), userRole);

        String text = """
        ✅ **РОЛЬ ВЫБРАНА**
            %s**
        
        Уважаемый пользователь,
        
        📋Прежде чем начать использование нашей платформы, пожалуйста, ознакомьтесь внимательно с правилами пользования услугами.
        Вы можете сделать это прямо сейчас, нажав на кнопку ниже:
        """.formatted(getRoleDisplay(userRole));

        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.ROLE_SELECTED);
        editMessage(userDto.getChatId(), userDto.getMessageId(), text, keyboard);
    }

    private void showFullRules(UserDto userDto) {
        userService.markRulesViewed(userDto.getChatId());
        String offerText = TextMessageService.publicOfferText();
        String rulesText = "Прочитайте правила:\n" +
                            offerText +
                            "✅ Нажатием кнопки «Принять правила» Пользователь подтверждает,\n" +
                            "что ознакомлен и согласен со всеми условиями настоящей Оферты.";
        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.RULES_VIEWED);
        editMessageWithQuote(userDto.getChatId(), userDto.getMessageId(), rulesText, "🔗 **ДОГОВОР-ОФЕРТА**", offerText.length(), keyboard);
    }

    private void acceptRules(UserDto userDto) {
        User user = userService.acceptRules(userDto.getChatId());

        // 🔥 ИЛИ УДАЛЯЕМ ВСЕ ЭКРАНЫ РЕГИСТРАЦИИ И ПРАВИЛ
        userSessionService.removeScreensOfType(userDto.getChatId(), "rules");
        userSessionService.removeScreensOfType(userDto.getChatId(), "register");

        String successText = """
                🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!
                
                🚀 Теперь вам доступен полный функционал платформы
                
                🏠Можете переходить на главный экран
                """;



        editMessage(userDto.getChatId(), userDto.getMessageId(), successText, keyboardFactory.createToMainMenuKeyboard());

        // 🔥 НОВАЯ ЛОГИКА - сбрасываем навигацию на главное меню
        userSessionService.pushToNavigationHistory(userDto.getChatId(), "main");
        userSessionService.setCurrentHandler(userDto.getChatId(), "menu");
        userSessionService.setCurrentAction(userDto.getChatId(), "menu", "main");
        log.info("🎉 User completed registration via callback: {}", userDto.getChatId());
    }


    private String getRegistrationStatusMessage(UserRole.RegistrationStatus status) {
        String mainText = """
            🚀 РЕГИСТРАЦИЯ НАЧАТА!
            
            Уважаемый пользователь, %s!
            
            📋Прежде чем начать использование нашей платформы, пожалуйста, ознакомьтесь внимательно с правилами пользования услугами.
            
            
            """;
        return switch (status) {
            case REGISTERED -> mainText + "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nОзнакомьтесь с правилами платформы";
            case RULES_VIEWED ->  mainText + "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nОзнакомьтесь с правилами платформы";
            case RULES_ACCEPTED -> "✅ Вы уже завершили регистрацию!";
            default -> "❌ Ошибка статуса";
        };
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
