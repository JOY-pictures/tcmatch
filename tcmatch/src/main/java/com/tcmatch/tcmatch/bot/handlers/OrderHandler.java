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
//public class OrderHandler extends BaseHandler {
//
//    public OrderHandler(KeyboardFactory keyboardFactory, UserSessionService userSessionService, BotExecutor botExecutor) {
//        super(botExecutor, keyboardFactory, userSessionService);
//    }
//
//    @Override
//    public boolean canHandle(String actionType, String action) {
//        return "order".equals(actionType);
//    }
//
//    @Override
//    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
//        String text = "<b>🚧 Раздел заказов в разработке...</b>";
//        editMessageWithHtml(chatId, messageId, text, keyboardFactory.createMainMenuKeyboard());
//        log.info("📦 Order action: {} for user {}", action, chatId);
//    }
//
//
//}