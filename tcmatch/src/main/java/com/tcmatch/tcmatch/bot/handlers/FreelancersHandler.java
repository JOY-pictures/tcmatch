package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.dto.BaseHandlerData;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
public class FreelancersHandler extends BaseHandler {
    public FreelancersHandler(KeyboardFactory keyboardFactory, UserSessionService userSessionService) {
        super(keyboardFactory, userSessionService);
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "freelancers".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        BaseHandlerData data = new BaseHandlerData(chatId, messageId, userName);

        switch (action) {
            case "show_menu":
                showFreelancersMenu(data);
                break;
            case "search":
                showFreelancerSearch(data);
                break;
            case "favorites":
                showFavoriteFreelancers(data);
                break;
            default:
                log.warn("❌ Unknown freelancers action: {}", action);
        }
    }

    public void showFreelancersMenu(BaseHandlerData data) {
        String text = """
            👥 **ПОИСК ИСПОЛНИТЕЛЕЙ**
            
            Выберите действие:
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createFreelancersMenuKeyboard();
        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
    }

    public void showFreelancerSearch(BaseHandlerData data) {
        String text = """
            🔍 **ПОИСК ИСПОЛНИТЕЛЕЙ**
            
            🚧 Раздел в разработке
            
            Здесь будет поиск и фильтрация исполнителей
            по специализации, рейтингу и опыту
            """;

        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
    }

    public void showFavoriteFreelancers(BaseHandlerData data) {
        String text = """
            ⭐ **ИЗБРАННЫЕ ИСПОЛНИТЕЛИ**
            
            🚧 Раздел в разработке
            
            Здесь будут ваши сохраненные исполнители
            """;

        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
    }
}
