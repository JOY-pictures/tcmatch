package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.service.NavigationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NavigationHandler extends BaseHandler{

    private final MenuHandler menuHandler;
    private final RegistrationHandler registrationHandler;
    private final UserProfileHandler userProfileHandler;
    private final ProjectsHandler projectsHandler;
    private final FreelancersHandler freelancersHandler;
    private final ApplicationHandler applicationHandler;
    private HelpHandler helpHandler;



    public NavigationHandler(KeyboardFactory keyboardFactory, NavigationService navigationService, MenuHandler menuHandler,
                             ProjectsHandler projectsHandler, RegistrationHandler registrationHandler, UserProfileHandler userProfileHandler,
                             FreelancersHandler freelancersHandler, ApplicationHandler applicationHandler){
        super(keyboardFactory, navigationService);
        this.menuHandler = menuHandler;
        this.projectsHandler = projectsHandler;
        this.registrationHandler = registrationHandler;
        this.userProfileHandler = userProfileHandler;
        this.freelancersHandler = freelancersHandler;
        this.applicationHandler = applicationHandler;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "navigation".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        if ("back".equals(action)) {
            handleBackNavigation(chatId, messageId);
        }
    }

    private void handleBackNavigation(Long chatId, Integer messageId) {
        String previousScreen = navigationService.popScreen(chatId);
        log.info("📱 Navigation back: {} -> {}", chatId, previousScreen);

        navigateToScreen(chatId, previousScreen, messageId);
    }

    private void navigateToScreen(Long chatId, String screen, Integer messageId) {
        log.debug("📱 Navigating to screen: {} for user {}", screen, chatId);

        // Если screen уже содержит "navigation:back" - это ошибка, показываем главное меню
        if (screen == null || screen.contains("navigation:back") || screen.trim().isEmpty()) {
            showMainMenu(chatId, messageId);
            return;
        }

        String[] screenParts = screen.split(":");
        String screenType = screenParts[0];
        String screenAction = screenParts.length > 1 ? screenParts[1] : "";
        String screenParam = screenParts.length > 2 ? screenParts[2] : null;

        log.debug("📱 Screen parsed - type: {}, action: {}, param: {}", screenType, screenAction, screenParam);

        // Проверяем, что screenAction не пустой
        if (screenAction.isEmpty()) {
            log.warn("⚠️ Empty screen action for screen: {}, showing main menu", screen);
            showMainMenu(chatId, messageId);
            return;
        }
        switch (screenType) {
            case "main":
                showMainMenu(chatId, messageId);
                break;
            case "menu":
                menuHandler.handle(chatId, screenAction, screenParam, messageId, "User");
                break;
            case "user_profile":
                userProfileHandler.handle(chatId, screenAction, screenParam, messageId, "User");
            case "projects":
                projectsHandler.handle(chatId, screenAction, screenParam, messageId, "User");
                break;
            case "project_search":
                // Делегируем обработку ProjectsHandler
                projectsHandler.handle(chatId, screenAction, screenParam, messageId, "User");
                break;
            case "application":
                applicationHandler.handle(chatId, screenAction, screenParam, messageId, "User");
                break;
            case "rules":
            case "register":
                registrationHandler.handle(chatId, screenAction, screenParam, messageId, "User");
                break;
            case "freelancers":
                freelancersHandler.handle(chatId, screenAction, screenParam, messageId, "User");
            case "help":
                helpHandler.handle(chatId, screenAction, screenParam, messageId, "User");
            default:
                log.warn("⚠️ Unknown screen type: {}, showing main menu", screenType);
                showMainMenu(chatId, messageId);
        }
    }
}
