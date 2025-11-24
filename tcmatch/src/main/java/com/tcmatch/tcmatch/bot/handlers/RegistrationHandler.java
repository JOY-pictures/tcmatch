//package com.tcmatch.tcmatch.bot.handlers;
//
//import com.tcmatch.tcmatch.bot.BotExecutor;
//import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
//import com.tcmatch.tcmatch.model.User;
//import com.tcmatch.tcmatch.model.dto.UserDto;
//import com.tcmatch.tcmatch.model.enums.UserRole;
//import com.tcmatch.tcmatch.service.TextMessageService;
//import com.tcmatch.tcmatch.service.UserService;
//import com.tcmatch.tcmatch.service.UserSessionService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.ResourceLoader;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//
//import java.util.Optional;
//
//@Component
//@Slf4j
//public class RegistrationHandler extends BaseHandler {
//    private final UserService userService;
//    private final ResourceLoader resourceLoader;
//
//    public RegistrationHandler(KeyboardFactory keyboardFactory, UserSessionService userSessionService,
//                               UserService userService, ResourceLoader resourceLoader, BotExecutor botExecutor) {
//        super(botExecutor, keyboardFactory, userSessionService);
//        this.userService = userService;
//        this.resourceLoader = resourceLoader;
//    }
//
//    @Override
//    public boolean canHandle(String actionType, String action) {
//        return "register".equals(actionType) || "rules".equals(actionType);
//    }
//
//    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
//            try {
//                // 🔥 ПРОВЕРЯЕМ СУЩЕСТВОВАНИЕ ПОЛЬЗОВАТЕЛЯ
//                Optional<UserDto> userOpt = userService.getUserDtoByChatId(chatId);
//
//                if (userOpt.isPresent()) {
//                    // Пользователь существует - используем DTO
//                    handleWithUserDto(action, parameter, userOpt.get());
//                } else {
//                    // 🔥 ПОЛЬЗОВАТЕЛЬ НЕ СУЩЕСТВУЕТ - СОЗДАЕМ НОВОГО
//                    UserDto newUser = userService.createNewUser(chatId, userName);
//                    handleWithUserDto(action, parameter, newUser);
//                }
//
//            } catch (Exception e) {
//                log.error("❌ Ошибка обработки команды: {}", e.getMessage());
//                // 🔥 ОТПРАВЛЯЕМ СООБЩЕНИЕ ОБ ОШИБКЕ ПОЛЬЗОВАТЕЛЮ
//                sendTemporaryErrorMessage(chatId, "Произошла ошибка. Попробуйте еще раз.", 5);
//            }
//        }
//
//    // 🔥 НОВЫЙ МЕТОД С USER DTO
//    public void handleWithUserDto(String action, String parameter, UserDto userDto) {
//        log.debug("📝 Handling registration for user: {}", userDto.getDisplayName());
//
//        switch (action) {
////            case "start":
////                startRegistration(userDto);
////                break;
////            case "view":
////                showRulesDoc(userDto);
////                break;
////            case "accept":
////                acceptRules(userDto);
////                break;
////            case "role":
////                handleRoleSelection(userDto, parameter);
////                break;
//            default:
//                log.warn("❌ Unknown registration action: {}", action);
//        }
//    }
//
//
////    // 🔥 ОБНОВЛЯЕМ МЕТОДЫ С USER DTO
////    private void startRegistration(UserDto userDto) {
////
////        UserRole.RegistrationStatus status = userService.getRegistrationStatus(userDto.getChatId());
////
////        if (status == UserRole.RegistrationStatus.REGISTERED) {
////            // 🔥 РЕГИСТРАЦИЯ УЖЕ ЗАВЕРШЕНА
////            String message = """
////            ✅ <b>Регистрация уже завершена</b>
////
////            Вы уже зарегистрированы в системе.
////            """;
////            InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
////            editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), message, keyboard);
////            return;
////        }
////
////        // 🔥 ПЕРЕДАЕМ ВСЕ ДАННЫЕ ПОЛЬЗОВАТЕЛЯ
////        User user = userService.registerFromTelegram(
////                userDto.getChatId(),
////                userDto.getUserName(),
////                userDto.getFirstName(),
////                userDto.getLastName()
////        );
////
////        // 🔥 ПОКАЗЫВАЕМ ВЫБОР РОЛИ
////        String text = """
////        🎯 <b>**ВЫБЕРИТЕ ВАШУ РОЛЬ**</b>
////
////        <i>Как вы планируете использовать платформу?</i>
////
////        👔 **ЗАКАЗЧИК** - размещаю проекты, ищу исполнителей
////        👨‍💻 **ИСПОЛНИТЕЛЬ** - ищу проекты, выполняю заказы
////
////        <u>💡 Вы сможете изменить роль позже в настройках</u>
////        """;
////
////        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.REGISTERED);
////        editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), text, keyboard);
////        log.info("🚀 Registration started via callback for: {}", userDto.getChatId());
////    }
//
////    // 🔥 ОБРАБОТКА ВЫБОРА РОЛИ
////    private void handleRoleSelection(UserDto userDto, String role) {
////        UserRole userRole = "customer".equals(role) ? UserRole.CUSTOMER : UserRole.FREELANCER;
////
////        User user = userService.updateUserRole(userDto.getChatId(), userRole);
////
////        String text = """
////        ✅ <b>**РОЛЬ ВЫБРАНА**</b>
////            <i>%s**</i>
////
////        Уважаемый пользователь,
////
////        <i>📋Прежде чем начать использование нашей платформы, пожалуйста, ознакомьтесь внимательно с правилами пользования услугами.
////        Вы можете сделать это прямо сейчас, нажав на кнопку ниже:</i>
////        """.formatted(getRoleDisplay(userRole));
////
////        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.ROLE_SELECTED);
////        editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), text, keyboard);
////    }
//
////    private void showRulesDoc(UserDto userDto) {
////        userService.markRulesViewed(userDto.getChatId());
////
////        String oferPath = "classpath:static/TCMatch-ofer.pdf";
////
////        Resource resource = resourceLoader.getResource(oferPath);
////
////        String rulesText = "<b>⬇\uFE0FПрочитайте правила⬇\uFE0F</b>\n\n" +
////
////                            "<i>✅ Нажатием кнопки «Принять правила» Пользователь подтверждает,\n" +
////                            "что ознакомлен и согласен со всеми условиями настоящей Оферты.</i>";
////        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(UserRole.RegistrationStatus.RULES_VIEWED);
////        Integer docMessageId = sendDocMessageReturnId(userDto.getChatId(), resource, "Документ-оферта.pdf");
////
////        if (docMessageId != null) {
////            userSessionService.addTemporaryMessageId(userDto.getChatId(), docMessageId);
////        }
////        editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), rulesText, keyboard);
////    }
////
////    private void acceptRules(UserDto userDto) {
////        User user = userService.acceptRules(userDto.getChatId());
////
////        deletePreviousMessages(user.getChatId());
////
////        // 🔥 ИЛИ УДАЛЯЕМ ВСЕ ЭКРАНЫ РЕГИСТРАЦИИ И ПРАВИЛ
////        userSessionService.removeScreensOfType(userDto.getChatId(), "rules");
////        userSessionService.removeScreensOfType(userDto.getChatId(), "register");
////
////        String successText = """
////                <b>🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!</b>
////
////                <i>🚀 Теперь вам доступен полный функционал платформы
////
////                🏠Можете переходить на главный экран</i>
////                """;
////
////
////
////        editMessageWithHtml(userDto.getChatId(), userSessionService.getMainMessageId(userDto.getChatId()), successText, keyboardFactory.createToMainMenuKeyboard());
////
////        // 🔥 НОВАЯ ЛОГИКА - сбрасываем навигацию на главное меню
////        userSessionService.pushToNavigationHistory(userDto.getChatId(), "main");
////        userSessionService.setCurrentHandler(userDto.getChatId(), "menu");
////        userSessionService.setCurrentAction(userDto.getChatId(), "menu", "main");
////        log.info("🎉 User completed registration via callback: {}", userDto.getChatId());
////    }
//
//
////    private String getRegistrationStatusMessage(UserRole.RegistrationStatus status) {
////        String mainText = """
////            <b>🚀 РЕГИСТРАЦИЯ НАЧАТА!</b>
////
////            Уважаемый пользователь, %s!
////
////            📋Прежде чем начать использование нашей платформы, пожалуйста, ознакомьтесь внимательно с правилами пользования услугами.
////
////
////            """;
////        return switch (status) {
////            case REGISTERED -> mainText + "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nОзнакомьтесь с правилами платформы";
////            case RULES_VIEWED ->  mainText + "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nОзнакомьтесь с правилами платформы";
////            case RULES_ACCEPTED -> "✅ Вы уже завершили регистрацию!";
////            default -> "❌ Ошибка статуса";
////        };
////    }
//
//
////    private String getRoleDisplay(UserRole role) {
////        return switch (role) {
////            case FREELANCER -> "👨‍💻 Исполнитель";
////            case CUSTOMER -> "👔 Заказчик";
////            case ADMIN -> "⚡ Администратор";
////            default -> "👤 Пользователь";
////        };
////    }
//}
