package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.service.NavigationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderHandler extends BaseHandler {

    public OrderHandler(KeyboardFactory keyboardFactory, NavigationService navigationService) {
        super(keyboardFactory, navigationService);
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "order".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        String text = "🚧 Раздел заказов в разработке...";
        editMessage(chatId, messageId, text, keyboardFactory.createMainMenuKeyboard());
        log.info("📦 Order action: {} for user {}", action, chatId);
    }
}