//package com.tcmatch.tcmatch.bot.handlers;
//
//import com.tcmatch.tcmatch.bot.BotExecutor;
//import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
//import com.tcmatch.tcmatch.service.UserSessionService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//public class NavigationHandler extends BaseHandler{
//
//    private final MenuHandler menuHandler;
//    private final RegistrationHandler registrationHandler;
//    private final UserProfileHandler userProfileHandler;
//    private final ProjectsHandler projectsHandler;
//    private final FreelancersHandler freelancersHandler;
//    private final ApplicationHandler applicationHandler;
//    private HelpHandler helpHandler;
//
//
//
//    public NavigationHandler(KeyboardFactory keyboardFactory, UserSessionService userSessionService, MenuHandler menuHandler,
//                             ProjectsHandler projectsHandler, RegistrationHandler registrationHandler, UserProfileHandler userProfileHandler,
//                             FreelancersHandler freelancersHandler, ApplicationHandler applicationHandler, BotExecutor botExecutor){
//        super(botExecutor, keyboardFactory, userSessionService);
//        this.menuHandler = menuHandler;
//        this.projectsHandler = projectsHandler;
//        this.registrationHandler = registrationHandler;
//        this.userProfileHandler = userProfileHandler;
//        this.freelancersHandler = freelancersHandler;
//        this.applicationHandler = applicationHandler;
//    }
//
//    @Override
//    public boolean canHandle(String actionType, String action) {
//        return "navigation".equals(actionType);
//    }
//
//    @Override
//    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
//        if ("back".equals(action)) {
//            handleBackNavigation(chatId, messageId);
//        }
//    }
//
//    private void handleBackNavigation(Long chatId, Integer messageId) {
//// 1. Сохраняем ключ текущего экрана ПЕРЕД тем, как его заменить
//        String currentScreen = userSessionService.getFromContext(chatId, "currentScreen", String.class);
//
//        // 2. Получаем предыдущий экран из стека
//        String previousScreen = userSessionService.popFromNavigationHistory(chatId);
//        log.info("📱 Navigation back: {} -> {}", chatId, previousScreen);
//
//        // 3. 🔥 ОЧИСТКА КОНТЕКСТА ТЕКУЩЕГО ЭКРАНА
//        // Например: если мы уходим с экрана 'projects:filter:', мы удаляем контекст фильтра.
//        if (currentScreen != null && !currentScreen.trim().isEmpty()) {
//
//            // ВАЖНО: Мы удаляем весь контекст, связанный с этим экраном.
//            // Здесь предполагается, что вы сохраняете контекст в UserSessionService
//            // под ключом, связанным с экраном (например, "projects:filter:context" или просто "projects:filter").
//
//            // Для упрощения, предположим, что контекстные данные хранятся под полным именем экрана.
//            userSessionService.remove(chatId, currentScreen);
//            log.debug("🗑️ Removed context data for screen: {}", currentScreen);
//        }
//
//        // 🔥 ОЧИЩАЕМ ВРЕМЕННЫЕ СООБЩЕНИЯ С ПРОЕКТАМИ ПЕРЕД НАВИГАЦИЕЙ
//        if (!userSessionService.getTemporaryMessageIds(chatId).isEmpty()) {
//            deletePreviousMessages(chatId);
//        }
//
//
//
//        // 🔥 ЕСЛИ ИСТОРИЯ ПУСТАЯ - ВОЗВРАЩАЕМ В ГЛАВНОЕ МЕНЮ
//        if (previousScreen == null) {
//            userSessionService.putToContext(chatId, "currentScreen", "main"); // 🔥 ОБНОВЛЯЕМ КОНТЕКСТ
//            showMainMenu(chatId, messageId);
//            return;
//        }
//
//        // 🔥 ОБНОВЛЯЕМ ТЕКУЩИЙ ЭКРАН В КОНТЕКСТЕ НА ТОТ, В КОТОРЫЙ ВОЗВРАЩАЕМСЯ
//        userSessionService.putToContext(chatId, "currentScreen", previousScreen);
//        log.debug("📱 Updated current screen after back navigation: {}", previousScreen);
//
//        navigateToScreen(chatId, previousScreen, messageId);
//    }
//
//    private void navigateToScreen(Long chatId, String screen, Integer clickedMessageId) {
//        log.debug("📱 Navigating to screen: {} for user {}", screen, chatId);
//
//        Integer messageId = userSessionService.getMainMessageId(chatId);
//
//        // Если screen уже содержит "navigation:back" - это ошибка, показываем главное меню
//        if (screen == null || screen.contains("navigation:back") || screen.trim().isEmpty()) {
//            log.warn("⚠️ Invalid screen: {}, showing main menu", screen);
//            showMainMenu(chatId, messageId);
//            return;
//        }
//
//        String[] screenParts = screen.split(":");
//        String screenType = screenParts[0];
//        String screenAction = screenParts.length > 1 ? screenParts[1] : "";
//        String screenParam = screen.length() > (screenType + ":" + screenAction).length()
//                ? screen.substring((screenType + ":" + screenAction + ":").length())
//                : null;
//
//        // Проверяем, что screenAction не пустой
//        if (screenAction.isEmpty()) {
//            log.warn("⚠️ Empty screen action for screen: {}, showing main menu", screen);
//            showMainMenu(chatId, messageId);
//            return;
//        }
//
//        log.debug("📱 Screen parsed - type: {}, action: {}, param: {}", screenType, screenAction, screenParam);
//
//
//        switch (screenType) {
//            case "main":
//                showMainMenu(chatId, messageId);
//                break;
//            case "menu":
//                menuHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//                break;
//            case "user_profile":
//                userProfileHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//            case "project":
//            case "project_search":
//                // Делегируем обработку ProjectsHandler
//                projectsHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//                break;
//            case "application":
//                applicationHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//                break;
//            case "rules":
//            case "register":
//                registrationHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//                break;
//            case "freelancers":
//                freelancersHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//            case "help":
//                helpHandler.handle(chatId, screenAction, screenParam, messageId, "User");
//            default:
//                log.warn("⚠️ Unknown screen type: {}, showing main menu", screenType);
//                showMainMenu(chatId, messageId);
//        }
//    }
//}
