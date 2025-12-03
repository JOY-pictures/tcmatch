package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import com.tcmatch.tcmatch.model.enums.UserState;
import com.tcmatch.tcmatch.service.ProjectSearchService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private OrderCreationState orderCreationState;
    private ProjectSearchService.SearchState searchState;
    private UserState userState;

    // 🔥 ВРЕМЕННЫЕ ДАННЫЕ ДЛЯ ВЕРИФИКАЦИИ
    private String pendingGitHubUrl;

    // 🔥 СИСТЕМНЫЕ ДАННЫЕ
    private Integer mainMessageId;
    private Integer lastPushMessageId; // <-- НОВОЕ ПОЛЕ
    private List<Integer> temporaryMessageIds;
    private Deque<String> navigationHistory;

    // 🔥 НОВОЕ: Платежные сообщения
    private List<PaymentMessageInfo> paymentMessages;

    public UserSession(Long chatId) {
        this.chatId = chatId;
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.context = new ConcurrentHashMap<>();
        this.temporaryMessageIds = new ArrayList<>();
        this.navigationHistory = new ArrayDeque<>();
        this.paymentMessages = new ArrayList<>(); // 🔥 Инициализируем
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

    // 🔥 НОВЫЕ МЕТОДЫ ДЛЯ ПЛАТЕЖНЫХ СООБЩЕНИЙ

    /**
     * Добавить платежное сообщение
     */
    public void addPaymentMessage(String paymentId, Integer messageId) {
        PaymentMessageInfo paymentMessage = new PaymentMessageInfo(paymentId, messageId);
        this.paymentMessages.add(paymentMessage);
    }

    /**
     * Найти платежное сообщение по paymentId
     */
    public Optional<PaymentMessageInfo> findPaymentMessage(String paymentId) {
        return this.paymentMessages.stream()
                .filter(pm -> paymentId.equals(pm.getPaymentId()))
                .findFirst();
    }

    /**
     * Удалить платежное сообщение по paymentId
     */
    public void removePaymentMessage(String paymentId) {
        this.paymentMessages.removeIf(pm -> paymentId.equals(pm.getPaymentId()));
    }

    /**
     * Получить все активные платежные сообщения
     */
    public List<PaymentMessageInfo> getActivePaymentMessages() {
        return new ArrayList<>(this.paymentMessages);
    }

    /**
     * Очистить все платежные сообщения
     */
    public void clearPaymentMessages() {
        this.paymentMessages.clear();
    }

    /**
     * Получить все messageId платежных сообщений
     */
    public List<Integer> getPaymentMessageIds() {
        return this.paymentMessages.stream()
                .map(PaymentMessageInfo::getMessageId)
                .filter(Objects::nonNull)
                .toList();
    }

    // 🔥 ВЛОЖЕННЫЙ КЛАСС ДЛЯ ХРАНЕНИЯ ИНФОРМАЦИИ О ПЛАТЕЖНОМ СООБЩЕНИИ
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentMessageInfo {
        private String paymentId;     // ID платежа в ЮKassa
        private Integer messageId;    // ID сообщения в Telegram
        private LocalDateTime createdAt;

        public PaymentMessageInfo(String paymentId, Integer messageId) {
            this.paymentId = paymentId;
            this.messageId = messageId;
            this.createdAt = LocalDateTime.now();
        }

        /**
         * Проверить, не истекло ли сообщение (больше 15 минут)
         */
        public boolean isExpired() {
            return createdAt.isBefore(LocalDateTime.now().minusMinutes(15));
        }
    }

    // 🔥 ПРОСТЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ СОСТОЯНИЕМ
    public void setWaitingForGitHub() {
        this.userState = UserState.WAITING_GITHUB_URL;
        this.pendingGitHubUrl = null;
    }

    public void clearState() {
        this.userState = UserState.NONE;
        this.pendingGitHubUrl = null;
    }

    public boolean isWaitingForGitHub() {
        return this.userState == UserState.WAITING_GITHUB_URL;
    }
}
