package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import com.tcmatch.tcmatch.service.ProjectSearchService;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class UserSession {
    private Long chatId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    // 🔥 ОСНОВНОЕ СОСТОЯНИЕ
    private String currentCommand;        // "projects", "application", "my_projects"
    private String currentAction;         // "search", "create", "edit"
    private Map<String, Object> context;  // Гибкие данные для любого хендлера

    // 🔥 СПЕЦИАЛИЗИРОВАННЫЕ СОСТОЯНИЯ
    private ProjectCreationState projectCreationState;
    private ApplicationCreationState applicationCreationState;
    private ProjectSearchService.SearchState searchState;

    // 🔥 СИСТЕМНЫЕ ДАННЫЕ
    private Integer mainMessageId;
    private Integer lastPushMessageId; // <-- НОВОЕ ПОЛЕ
    private List<Integer> temporaryMessageIds;
    private Deque<String> navigationHistory;

    public UserSession(Long chatId) {
        this.chatId = chatId;
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.context = new ConcurrentHashMap<>();
        this.temporaryMessageIds = new ArrayList<>();
        this.navigationHistory = new ArrayDeque<>();
    }

    // 🔥 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void addTemporaryMessageId(Integer messageId) {
        if (messageId != null) {
            this.temporaryMessageIds.add(messageId);
        }
    }

    public void clearTemporaryMessages() {
        this.temporaryMessageIds.clear();
    }

    public void pushToHistory(String screen) {
        this.navigationHistory.push(screen);
    }

    public String popFromHistory() {
        return this.navigationHistory.isEmpty() ? null : this.navigationHistory.pop();
    }

    public String peekHistory() {
        return this.navigationHistory.isEmpty() ? null : this.navigationHistory.peek();
    }

    // 🔥 МЕТОДЫ ДЛЯ РАБОТЫ С КОНТЕКСТОМ
    public void putToContext(String key, Object value) {
        this.context.put(key, value);
    }

    public Object getFromContext(String key) {
        return this.context.get(key);
    }

    public <T> T getFromContext(String key, Class<T> type) {
        Object value = this.context.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void removeFromContext(String key) {
        this.context.remove(key);
    }

    public void clearContext() {
        this.context.clear();
    }

    // 🔥 ПРОВЕРКИ СОСТОЯНИЯ
    public boolean isInHandler(String handler) {
        return handler.equals(this.currentCommand);
    }

    public boolean isInAction(String handler, String action) {
        return handler.equals(this.currentCommand) && action.equals(this.currentAction);
    }

    public boolean hasProjectCreationState() {
        return this.projectCreationState != null;
    }

    public boolean hasApplicationCreationState() {
        return this.applicationCreationState != null;
    }
}
