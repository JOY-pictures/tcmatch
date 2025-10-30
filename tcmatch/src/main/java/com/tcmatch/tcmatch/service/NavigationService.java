package com.tcmatch.tcmatch.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NavigationService {
    private final Map<Long, Deque<String>> userNavigationHistory = new ConcurrentHashMap<>();

//    * @param chatId ID чата пользователя
//     * @param screen Идентификатор экрана (например: "menu:profile", "project:details:123")

    public void showHistory(Long chatId) {
        System.out.println(userNavigationHistory.get(chatId));
    }

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

        System.out.println(history);
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
        Deque<String> newHistory = new ArrayDeque<>();
        newHistory.push("main");
        userNavigationHistory.put(chatId, newHistory);
        log.debug("📱 Reset to main for user {}", chatId);
    }

    //Очистить историю до определенной глубины
    public void clearHistoryBeyondDepth(Long chatId, int maxDepth) {
        Deque<String> history = userNavigationHistory.get(chatId);
        if (history != null && history.size() > maxDepth) {
            Deque<String> newHistory = new ArrayDeque<>();
            Object[] array = history.toArray();
            for (int i = array.length-1; i >= array.length - maxDepth; i--) {
                newHistory.push((String) array[i]);
            }
            userNavigationHistory.put(chatId, newHistory);
            log.debug("📱 Cleared history beyond depth {} for user {}", maxDepth, chatId);
        }
    }

    //Удалить экраны определенного типа из истории
    public void removeScreenOfType(Long chatId, String screenType) {
        Deque<String> history = userNavigationHistory.get(chatId);
        if (history != null) {
            Deque<String> newHistory = new ArrayDeque<>();
            // Сохраняем порядок используя iterator в правильном порядке
            for (Iterator<String> it = history.descendingIterator(); it.hasNext(); ) {
                String screen = it.next();
                if (!screen.startsWith(screenType + ":")) {
                    newHistory.push(screen);
                }
            }
            userNavigationHistory.put(chatId, newHistory);
            log.debug("📱 Removed screens of type {} for user {}", screenType, chatId);
        }
    }
}
