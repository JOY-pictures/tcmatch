package com.tcmatch.tcmatch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.swing.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class NavigationService {

    private final UserSessionService userSessionService;

    public void saveToNavigationHistory(Long chatId, String actionType, String action, String parameter) {

        System.out.println(userSessionService.getUserHistory(chatId));

        log.debug("📱 Navigation history - Type: {}, Action: {}, Param: {}", actionType, action, parameter);

        // 🔥 ПОЛУЧАЕМ ТЕКУЩИЙ ЭКРАН ИЗ СЕССИИ
        String currentScreen = userSessionService.getFromContext(chatId, "currentScreen", String.class);

        // 🚫 НЕ СОХРАНЯЕМ ТЕКУЩИЙ ЭКРАН ПРИ НАВИГАЦИИ "НАЗАД"
        if ("navigation".equals(actionType) && "back".equals(action)) {
            log.debug("📱 Skipping history save for BACK navigation");
            return;
        }

        // 🚫 НЕ СОХРАНЯЕМ ТЕКУЩИЙ ЭКРАН ПРИ НАВИГАЦИИ "НАЗАД"
        if ("subscription".equals(actionType) && "select".equals(action)) {
            log.debug("📱 Skipping history save for select subscription");
            return;
        }

        if ("notification".equals(actionType) && ("delete".equals(action) || "view".equals(action))) {
            log.debug("📱 Skipping history save for delete message");
            return;
        }

        if ("admin".equals(actionType)) {
            log.debug("📱 Skipping history save for admin action");
            return;
        }

        if ("pagination".equals(action)) {
            log.debug("📱 Skipping history save for pagination");
            return;
        }

        // 🔥 ПЕРЕХОД НА ГЛАВНЫЙ ЭКРАН - СБРАСЫВАЕМ ИСТОРИЮ
        if ("menu".equals(actionType) && "main".equals(action)) {
            log.debug("📱 Reset history for MAIN menu navigation");
            userSessionService.resetToMain(chatId);
            return;
        }

        if ("project".equals(actionType) && "favorite".equals(action)) {
            log.debug("📱 Skipping history save for favorite");
            return;
        }

        // 🔥 ЕСЛИ ЭТО ТОТ ЖЕ ACTION - ПРОСТО ОБНОВЛЯЕМ КОНТЕКСТ БЕЗ СОХРАНЕНИЯ
        if (currentScreen != null && isSameAction(currentScreen, actionType, action)) {
            log.debug("📱 Same action {} - updating context without history", action);
            String newScreen = buildScreenKey(actionType, action, parameter);
            userSessionService.putToContext(chatId, "currentScreen", newScreen);
            return;
        }

        String newScreen = buildScreenKey(actionType, action, parameter);

        // ✅ РАЗНЫЙ ACTION - ВСЕГДА СОХРАНЯЕМ ТЕКУЩИЙ В ИСТОРИЮ
        if (currentScreen != null && !currentScreen.isEmpty()) {
            userSessionService.pushToNavigationHistory(chatId, currentScreen);
            log.debug("📱 Saved current screen to history: {}", currentScreen);
        }

        System.out.println(userSessionService.getUserHistory(chatId));


        // 🔥 ОБНОВЛЯЕМ ТЕКУЩИЙ ЭКРАН НА НОВЫЙ
        userSessionService.putToContext(chatId, "currentScreen", newScreen);
        log.debug("📱 Updated current screen: {}", newScreen);
    }

    private String buildScreenKey(String actionType, String action, String parameter) {
        return actionType + ":" + action + (parameter != null ? ":" + parameter : "");
    }

    private boolean isSameAction(String currentScreen, String actionType, String newAction) {
        if (currentScreen == null) return false;
        String[] parts = currentScreen.split(":");
        return parts.length >= 2 && parts[1].equals(newAction) && actionType.equals(parts[0]);
    }

    public String getPreviousScreen(Long chatId) {
        return userSessionService.popFromNavigationHistory(chatId);
    }

    public String getCurrentScreen(Long chatId) {
        return userSessionService.getFromContext(chatId, "currentScreen", String.class);
    }

    public void clearHistory(Long chatId) {
        userSessionService.resetToMain(chatId);
    }
}