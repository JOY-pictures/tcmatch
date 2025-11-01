package com.tcmatch.tcmatch.bot.handlers;


import com.tcmatch.tcmatch.model.UserSession;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CallbackHandler {

    private final UserSessionService userSessionService;
    private final List<BaseHandler> handlers;
    private final Map<Long, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long CLICK_COOLDOWN_MS = 500;

    public CallbackHandler(List<BaseHandler> handlers, UserSessionService userSessionService) {
        this.handlers = handlers;
        this.userSessionService = userSessionService;
    }

    public void setSender(AbsSender sender) {
        handlers.forEach(handler -> handler.setSender(sender));
    }

    public void handleCallback(Long chatId, String callbackData, String userName, Integer messageId) {

        // Защита от спама
        if (isClickCooldown(chatId)) return;

        log.info("🔄 Handling callback: {} from user {}", callbackData, chatId);

        String[] parts = callbackData.split(":", 3);
        String actionType = parts[0];
        String action = parts[1];
        String parameter = parts.length > 2 ? parts[2] : null;

        saveToNavigationHistory(chatId, actionType, action, parameter);

        for (BaseHandler handler : handlers) {
            if (handler.canHandle(actionType, action)) {
                handler.handle(chatId, action, parameter, messageId, userName);
                return;
            }
        }

        // Если не нашли обработчик
        log.warn("⚠️ No handler found for: {}:{}", actionType, action);
    }

    private boolean isClickCooldown(Long chatId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickTime.get(chatId);

        if (lastTime != null && (currentTime - lastTime) < CLICK_COOLDOWN_MS) {
            log.debug("⏳ Click cooldown for user: {}", chatId);
            return true; // Игнорируем быстрое повторное нажатие
        }

        lastClickTime.put(chatId, currentTime);
        return false;

    }

    //Сохраняет действие в историю навигации (если нужно)
    private void saveToNavigationHistory(Long chatId, String actionType, String action, String parameter) {

        // 🚫 НЕ СОХРАНЯЕМ ТЕКУЩИЙ ЭКРАН ПРИ НАВИГАЦИИ "НАЗАД"
        if ("navigation".equals(actionType) && "back".equals(action)) {
            log.debug("📱 Skipping history save for BACK navigation");
            return;
        }

        //🔥 ПЕРЕХОД НА ГЛАВНЫЙ ЭКРАН - СБРАСЫВАЕМ ИСТОРИЮ
        if ("menu".equals(actionType) && "main".equals(action)) {
            log.debug("📱 Reset history for MAIN menu navigation");
            userSessionService.resetToMain(chatId); // 🔥 СБРАСЫВАЕМ ИСТОРИЮ
            return;
        }

        // 🔥 ПОЛУЧАЕМ ТЕКУЩИЙ ЭКРАН ИЗ СЕССИИ
        String currentScreen = userSessionService.getFromContext(chatId, "currentScreen", String.class);

        // 🔥 ЕСЛИ ЭТО ТОТ ЖЕ ACTION - ПРОСТО ОБНОВЛЯЕМ КОНТЕКСТ БЕЗ СОХРАНЕНИЯ
        if (currentScreen != null && isSameAction(currentScreen, action)) {
            log.debug("📱 Same action {} - updating context without history", action);
            String newScreen = actionType + ":" + action + (parameter != null ? ":" + parameter : "");
            userSessionService.putToContext(chatId, "currentScreen", newScreen);
            return;
        }

        // ✅ РАЗНЫЙ ACTION - СОХРАНЯЕМ ТЕКУЩИЙ В ИСТОРИЮ
        if (currentScreen != null && !currentScreen.isEmpty()) {
            userSessionService.pushToNavigationHistory(chatId, currentScreen);
            log.debug("📱 Saved current screen to history: {}", currentScreen);
        }

        // 🔥 ОБНОВЛЯЕМ ТЕКУЩИЙ ЭКРАН НА НОВЫЙ
        String newScreen = actionType + ":" + action + (parameter != null ? ":" + parameter : "");
        userSessionService.putToContext(chatId, "currentScreen", newScreen);
        log.debug("📱 Updated current screen: {}", newScreen);
    }

    // 🔥 ПРОВЕРЯЕМ, ЭТО ТОТ ЖЕ ACTION (просто разные параметры)
    private boolean isSameAction(String currentScreen, String newAction) {
        if (currentScreen == null) return false;

        // 🔥 ИЗВЛЕКАЕМ ACTION ИЗ ТЕКУЩЕГО ЭКРАНА
        String[] parts = currentScreen.split(":");
        if (parts.length >= 2) {
            String currentAction = parts[1]; // filter, pagination, search и т.д.
            return currentAction.equals(newAction);
        }
        return false;
    }
}