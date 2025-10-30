package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.dto.BaseHandlerData;
import com.tcmatch.tcmatch.model.dto.ProjectData;
import com.tcmatch.tcmatch.model.dto.UserProfileData;
import com.tcmatch.tcmatch.service.NavigationService;
import com.tcmatch.tcmatch.service.ProjectSearchService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
public class MenuHandler extends BaseHandler{
    private final UserService userService;
    private ProjectsHandler projectsHandler;
    private final ProjectSearchService projectSearchService;
    private final UserProfileHandler userProfileHandler;
    private final HelpHandler helpHandler;
    private final FreelancersHandler freelancersHandler;

    public MenuHandler(KeyboardFactory keyboardFactory, NavigationService navigationService,
                       UserService userService, ProjectSearchService projectSearchService, UserProfileHandler userProfileHandler,
                        ProjectsHandler projectsHandler, HelpHandler helpHandler, FreelancersHandler freelancersHandler) {
        super(keyboardFactory, navigationService);
        this.userService = userService;
        this.projectSearchService = projectSearchService;
        this.userProfileHandler = userProfileHandler;
        this.projectsHandler = projectsHandler;
        this.helpHandler = helpHandler;
        this.freelancersHandler = freelancersHandler;
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "menu".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        switch (action) {
            case "main": // Добавим обработку для прямого перехода в главное меню
                showMainMenu(chatId, messageId); // Используем метод из BaseHandler
                break;
            case "profile":
                // Делегируем обработку UserProfileHandler
                userProfileHandler.showUserProfile(new UserProfileData(chatId, messageId, userName));
                break;
            case "projects":
                projectsHandler.showProjectsMenu(new ProjectData(chatId, messageId, userName));
                showProjectsSearch(chatId, messageId, "");
                break;
            case "freelancers":
                freelancersHandler.showFreelancersMenu(new BaseHandlerData(chatId, messageId, userName));
                break;
            case "help":
                helpHandler.showHelpMenu(new BaseHandlerData(chatId, messageId, userName));
                break;
            default:
                log.warn("❌ Unknown menu action: {}", action);
        }
    }

    private void showProjectsSearch(Long chatId, Integer messageId, String filter) {

    }
}
