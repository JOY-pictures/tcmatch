package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.NavigationService;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
public class RegistrationHandler extends BaseHandler {
    private final UserService userService;

    public RegistrationHandler(KeyboardFactory keyboardFactory, NavigationService navigationService, UserService userService) {
        super(keyboardFactory, navigationService);
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
        String text = """
            🚀 РЕГИСТРАЦИЯ НАЧАТА!
            
            Уважаемый пользователь, %s!
            
            📋Прежде чем начать использование нашей платформы, пожалуйста, ознакомьтесь внимательно с правилами пользования услугами.
            Вы можете сделать это прямо сейчас, нажав на кнопку ниже:
            """.formatted(userDto.getDisplayName());

        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.REGISTERED);
        editMessage(userDto.getChatId(), userDto.getMessageId(), text, keyboard);
        log.info("🚀 Registration started via callback for: {}", userDto.getChatId());
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

        navigationService.removeScreenOfType(userDto.getChatId(), "rules");
        navigationService.removeScreenOfType(userDto.getChatId(), "register");

        String successText = """
                🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!
                
                🚀 Теперь вам доступен полный функционал платформы
                
                🏠Можете переходить на главный экран
                """;



        editMessage(userDto.getChatId(), userDto.getMessageId(), successText, keyboardFactory.createToMainMenuKeyboard());

        // Сбрасываем навигацию на главное меню
        navigationService.resetToMain(userDto.getChatId());
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


}
