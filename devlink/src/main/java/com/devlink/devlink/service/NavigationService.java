package com.devlink.devlink.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NavigationService {
    private final Map<Long, Deque<String>> userNavigationHistory = new ConcurrentHashMap<>();

//    * @param chatId ID чата пользователя
//     * @param screen Идентификатор экрана (например: "menu:profile", "project:details:123")

    public void pushScreen(Long chatId, String screen) {
        Deque<String> history = userNavigationHistory.computeIfAbsent(chatId, k -> new ArrayDeque<>());
        history.push(screen);
        log.debug("📱 Navigation: user {} -> {}", chatId, screen);
    }

    public String popScreen(Long chatId) {
        Deque<String> history = userNavigationHistory.get(chatId);
        if (history == null || history.isEmpty()) {
            log.debug("📱 Navigation: user {} has no history, returning to main", chatId);
            return "main";
        }
        System.out.println(history);
        String currentScreen = history.pop();
        log.debug("📱 Navigation: user {} leaving {}", chatId, currentScreen);

        if (history.isEmpty()) {
            log.debug("📱 Navigation: user {} history empty, returning to main", chatId);
            return "main";
        }


        String previousScreen = history.peek();
        log.debug("📱 Navigation: user {} -> {}", chatId, previousScreen);
        return previousScreen;
    }

    public String getCurrentScreen(Long chatId) {
        Deque<String> history = userNavigationHistory.get(chatId);
        return (history != null && !history.isEmpty()) ? history.peek() : null;
    }

    public void clearHistory(Long chatId) {
        userNavigationHistory.remove(chatId);
        log.debug("📱 Navigation: cleared history for user {}", chatId);
    }

    public void resetToMain(Long chatId) {
        clearHistory(chatId);
        pushScreen(chatId, "main");
    }
}
