package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.model.UserSession;
import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserSessionService {

    @Lazy
    @Autowired
    private BotExecutor botExecutor;

    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    // 🔥 ОСНОВНЫЕ МЕТОДЫ ДОСТУПА К СЕССИИ

    public UserSession getSession(Long chatId) {
        return userSessions.computeIfAbsent(chatId, k -> {
            log.info("🆕 Created new session for user: {}", chatId);
            return new UserSession(chatId);
        });
    }

    public UserSession getSessionAndUpdateActivity(Long chatId) {
        UserSession session = getSession(chatId);
        session.updateActivity();
        return session;
    }

    public boolean hasSession(Long chatId) {
        return userSessions.containsKey(chatId);
    }

    // 🔥 УПРАВЛЕНИЕ СОСТОЯНИЕМ

    public void setCurrentCommand(Long chatId, String command) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.setCurrentCommand(command);
        log.debug("🔧 User {} handler set to: {}", chatId, command);
    }

    public String getCurrentCommand(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getCurrentCommand(); // 🔥 Возвращаем поле currentHandler
    }

    public void setCurrentAction(Long chatId, String command, String action) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.setCurrentCommand(command);
        session.setCurrentAction(action);
        log.debug("🔧 User {} action set to: {}/{}", chatId, command, action);
    }

    public String getCurrentAction(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getCurrentAction(); // 🔥 И currentAction тоже
    }

    public void clearState(Long chatId) {
        UserSession session = getSession(chatId);
        session.setCurrentCommand(null);
        session.setCurrentAction(null);
        session.clearContext();
        session.clearTemporaryMessages();
        log.debug("🧹 Cleared state for user: {}", chatId);
    }

    public void clearCommandState(Long chatId, String command) {
        UserSession session = getSession(chatId);
        if (command.equals(session.getCurrentCommand())) {
            session.setCurrentCommand(null);
            session.setCurrentAction(null);
            session.clearContext();
            log.debug("🧹 Cleared {} state for user: {}", command, chatId);
        }
    }

    // 🔥 РАБОТА С СООБЩЕНИЯМИ

    public void setMainMessageId(Long chatId, Integer messageId) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.setMainMessageId(messageId);
        log.debug("💾 Set main message ID for {}: {}", chatId, messageId);
    }

    public Integer getMainMessageId(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getMainMessageId();
    }

    public void addTemporaryMessageId(Long chatId, Integer messageId) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.addTemporaryMessageId(messageId);
        log.debug("📝 Added temporary message ID for {}: {}", chatId, messageId);
    }

    public List<Integer> getTemporaryMessageIds(Long chatId) {
        UserSession session = getSession(chatId);
        return new ArrayList<>(session.getTemporaryMessageIds());
    }

    public void clearTemporaryMessages(Long chatId) {
        UserSession session = getSession(chatId);
        List<Integer> messageIds = session.getTemporaryMessageIds();
        if (!messageIds.isEmpty()) {
            log.debug("🗑️ Clearing {} temporary messages for user: {}", messageIds.size(), chatId);
        }
        session.clearTemporaryMessages();
    }

    // 🔥 РАБОТА С КОНТЕКСТОМ

    public void putToContext(Long chatId, String key, Object value) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        // 🔥 БЕЗОПАСНОЕ ЛОГИРОВАНИЕ - ИЗБЕГАЕМ toString() НА HIBERNATE ПРОКСИ
        if (value instanceof List) {
            log.debug("💾 Context put for {}: {} = List[{} elements]", chatId, key, ((List<?>) value).size());
        } else {
            log.debug("💾 Context put for {}: {} = {}", chatId, key,
                    value != null ? value.getClass().getSimpleName() : "null");
        }

        session.putToContext(key, value);
    }

    public Object getFromContext(Long chatId, String key) {
        UserSession session = getSession(chatId);
        return session.getFromContext(key);
    }

    public <T> T getFromContext(Long chatId, String key, Class<T> type) {
        UserSession session = getSession(chatId);
        return session.getFromContext(key, type);
    }

    // 🔥 СПЕЦИАЛИЗИРОВАННЫЕ СОСТОЯНИЯ

    public void setProjectCreationState(Long chatId, ProjectCreationState state) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.setProjectCreationState(state);
        log.debug("🏗️ Set project creation state for user: {}", chatId);
    }

    public ProjectCreationState getProjectCreationState(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getProjectCreationState();
    }

    public void clearProjectCreationState(Long chatId) {
        UserSession session = getSession(chatId);
        session.setProjectCreationState(null);
        log.debug("🧹 Cleared project creation state for user: {}", chatId);
    }

    public void setApplicationCreationState(Long chatId, ApplicationCreationState state) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.setApplicationCreationState(state);
        log.debug("📝 Set application creation state for user: {}", chatId);
    }

    public ApplicationCreationState getApplicationCreationState(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getApplicationCreationState();
    }

    public void clearApplicationCreationState(Long chatId) {
        UserSession session = getSession(chatId);
        session.setApplicationCreationState(null);
        log.debug("🧹 Cleared application creation state for user: {}", chatId);
    }

    public void pushToNavigationHistory(Long chatId, String screen) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.pushToHistory(screen);
        System.out.println(session.getNavigationHistory());
        log.debug("🧭 Navigation history pushed for {}: {}", chatId, screen);
    }

    public String popFromNavigationHistory(Long chatId) {
        UserSession session = getSession(chatId);
        return session.popFromHistory();
    }

    public String peekNavigationHistory(Long chatId) {
        UserSession session = getSession(chatId);
        return session.peekHistory();
    }

    // 🔥 МЕТОД ДЛЯ УДАЛЕНИЯ ОПРЕДЕЛЕННОЙ ГЛУБИНЫ ИСТОРИИ
    public void clearHistoryBeyondDepth(Long chatId, int maxDepth) {
        UserSession session = getSession(chatId);
        Deque<String> history = session.getNavigationHistory();

        if (history != null && history.size() > maxDepth) {
            Deque<String> newHistory = new ArrayDeque<>();

            // 🔥 СОХРАНЯЕМ ТОЛЬКО ПОСЛЕДНИЕ maxDepth ЭЛЕМЕНТОВ
            Iterator<String> iterator = history.iterator();
            for (int i = 0; i < maxDepth && iterator.hasNext(); i++) {
                newHistory.push(iterator.next());
            }

            session.setNavigationHistory(newHistory);
            log.debug("📱 Cleared history beyond depth {} for user {}", maxDepth, chatId);
        }
    }

    // 🔥 МЕТОД ДЛЯ УДАЛЕНИЯ ЭКРАНОВ ОПРЕДЕЛЕННОГО ТИПА
    public void removeScreensOfType(Long chatId, String screenType) {
        UserSession session = getSession(chatId);
        Deque<String> history = session.getNavigationHistory();

        if (history != null && !history.isEmpty()) {
            Deque<String> newHistory = new ArrayDeque<>();

            // 🔥 СОХРАНЯЕМ ТОЛЬКО ЭКРАНЫ, КОТОРЫЕ НЕ НАЧИНАЮТСЯ С screenType
            for (String screen : history) {
                if (!screen.startsWith(screenType + ":")) {
                    newHistory.push(screen);
                }
            }

            session.setNavigationHistory(newHistory);
            log.debug("📱 Removed screens of type {} for user {}", screenType, chatId);
        }
    }

    // 🔥 МЕТОД ДЛЯ ПОЛНОЙ ОЧИСТКИ ИСТОРИИ С СОХРАНЕНИЕМ ГЛАВНОГО ЭКРАНА
    public void resetToMain(Long chatId) {
        UserSession session = getSession(chatId);

        // 🔥 СБРАСЫВАЕМ ИСТОРИЮ НАВИГАЦИИ
        session.setNavigationHistory(new ArrayDeque<>());

        // 🔥 УСТАНАВЛИВАЕМ ТЕКУЩИЙ ЭКРАН НА ГЛАВНЫЙ
        putToContext(chatId, "currentScreen", "main:menu");

        // 🔥 ОЧИЩАЕМ ВРЕМЕННЫЕ СООБЩЕНИЯ
        clearTemporaryMessages(chatId);

        // 🔥 ОЧИЩАЕМ СПЕЦИАЛИЗИРОВАННЫЕ СОСТОЯНИЯ
        clearApplicationCreationState(chatId);
        clearProjectCreationState(chatId);

        log.debug("📱 Reset to main - cleared history and states for user: {}", chatId);
    }

    /**
     * 🔥 СБРАСЫВАЕТ ИСТОРИЮ НАВИГАЦИИ ПОЛЬЗОВАТЕЛЯ
     */
    public void clearNavigationHistory(Long chatId) {
        try {
            // Очищаем историю вкладок/навигации
            // Зависит от того, как у вас реализована навигация
            userSessions.computeIfPresent(chatId, (key, session) -> {
                session.setNavigationHistory(new ArrayDeque<>());
                return session;
            });

            log.debug("🧹 История навигации очищена для пользователя {}", chatId);

        } catch (Exception e) {
            log.warn("⚠️ Не удалось очистить историю навигации для пользователя {}: {}", chatId, e.getMessage());
        }
    }

    // 🔥 АВТООЧИСТКА СТАРЫХ СЕССИЙ

//    @Scheduled(fixedRate = 600000) // 10 минут
//    public void cleanupOldSessions() {
//        synchronized (userSessions) {
//            LocalDateTime cutoffTime = LocalDateTime.now().minus(1, ChronoUnit.HOURS);
//            int initialSize = userSessions.size();
//
//            userSessions.entrySet().removeIf(entry -> {
//                UserSession session = entry.getValue();
//                boolean shouldRemove = session.getLastActivityAt().isBefore(cutoffTime);
//                if (shouldRemove) {
//                    log.debug("🧹 Removing old session for user: {} (last activity: {})",
//                            entry.getKey(), session.getLastActivityAt());
//                }
//                return shouldRemove;
//            });
//
//            int finalSize = userSessions.size();
//            if (initialSize != finalSize) {
//                log.info("🧹 Session cleanup: {} -> {} sessions (removed: {})",
//                        initialSize, finalSize, initialSize - finalSize);
//            }
//        }
//    }

    // 🔥 ДИАГНОСТИКА

    public void printSessionState(Long chatId) {
        if (hasSession(chatId)) {
            UserSession session = getSession(chatId);
            log.info("🔍 Session state for {}: handler={}, action={}, context={}, tempMessages={}",
                    chatId, session.getCurrentCommand(), session.getCurrentAction(),
                    session.getContext().size(), session.getTemporaryMessageIds().size());
        } else {
            log.info("🔍 No session found for user: {}", chatId);
        }
    }

    public Map<Long, UserSession> getAllSessions() {
        return new ConcurrentHashMap<>(userSessions);
    }

    /**
     * 🔥 Очистить все сессии (вызывать при shutdown)
     */
    public void clearAllSessions() {
        synchronized (userSessions) {
            int size = userSessions.size();
            userSessions.clear();
            log.info("🧹 Очищены все сессии пользователей: {}", size);
        }
    }

    public Deque<String> getUserHistory(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getNavigationHistory();
    }

    public void remove(Long chatId, String key) {
        UserSession session = userSessions.get(chatId); // Получаем объект сессии

        if (session != null) {
            // 🔥 Вызываем ваш существующий метод из UserSession:
            session.removeFromContext(key);
            log.debug("🗑️ Removed context key '{}' for user {}", key, chatId);
        }
    }

    public List<Integer> getAndClearTemporaryMessageIds(Long chatId) {
        UserSession session = userSessions.get(chatId);
        if (session == null) {
            return Collections.emptyList();
        }

        // Получаем текущий список ID
        List<Integer> messageIds = session.getTemporaryMessageIds();

        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Создаем копию списка для возврата
        List<Integer> idsToDelete = new ArrayList<>(messageIds);

        // 🔥 Очищаем список ID в UserSession, чтобы не удалять их повторно
        session.clearTemporaryMessages();

        return idsToDelete;
    }

    public Integer getLastPushMessageId(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getLastPushMessageId();
    }

    public void setLastPushMessageId(Long chatId, Integer messageId) {
        UserSession session = getSession(chatId);
        session.setLastPushMessageId(messageId);
    }

    public void setOrderCreationState(Long chatId, OrderCreationState state) {
        UserSession session = getSession(chatId);
        session.setOrderCreationState(state);
    }

    public OrderCreationState getOrderCreationState(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getOrderCreationState();
    }

    public void clearOrderCreationState(Long chatId) {
        UserSession session = getSession(chatId);
        session.setOrderCreationState(null);
    }

    public void clearCurrentCommand(Long chatId) {
        // 1. Получаем объект сессии (предполагаем, что есть такой метод)
        UserSession session = getSession(chatId);

        if (session != null && session.getCurrentCommand() != null) {
            // 2. Устанавливаем команду в null (или пустую строку, в зависимости от реализации)
            session.setCurrentCommand(null);

            log.info("✅ Cleared current command for user {}", chatId);
        }
    }

    // 🔥 НОВЫЕ МЕТОДЫ ДЛЯ ПЛАТЕЖНЫХ СООБЩЕНИЙ

    /**
     * Добавить платежное сообщение
     */
    public void addPaymentMessage(Long chatId, String paymentId, Integer messageId) {
        UserSession session = getSessionAndUpdateActivity(chatId);
        session.addPaymentMessage(paymentId, messageId);
        log.info("💳 Добавлено платежное сообщение: chatId={}, paymentId={}, messageId={}",
                chatId, paymentId, messageId);
    }

    /**
     * Найти платежное сообщение по paymentId
     */
    public Optional<UserSession.PaymentMessageInfo> findPaymentMessage(Long chatId, String paymentId) {
        UserSession session = getSession(chatId);
        return session.findPaymentMessage(paymentId);
    }

    /**
     * Удалить платежное сообщение
     */
    public void removePaymentMessage(Long chatId, String paymentId) {
        UserSession session = getSession(chatId);
        session.removePaymentMessage(paymentId);
        log.info("🗑️ Удалено платежное сообщение: chatId={}, paymentId={}", chatId, paymentId);
    }

    /**
     * Получить все платежные сообщения пользователя
     */
    public List<UserSession.PaymentMessageInfo> getPaymentMessages(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getActivePaymentMessages();
    }

    /**
     * Получить все ID платежных сообщений для удаления
     */
    public List<Integer> getPaymentMessageIds(Long chatId) {
        UserSession session = getSession(chatId);
        return session.getPaymentMessageIds();
    }

    /**
     * Очистить все платежные сообщения пользователя
     */
    public void clearPaymentMessages(Long chatId) {
        UserSession session = getSession(chatId);
        List<UserSession.PaymentMessageInfo> messages = session.getActivePaymentMessages();

        if (!messages.isEmpty()) {
            log.info("🧹 Очистка {} платежных сообщений для chatId={}",
                    messages.size(), chatId);
            session.clearPaymentMessages();
        }
    }

    /**
     * Удалить истекшие платежные сообщения
     */
    public void cleanupExpiredPaymentMessages(Long chatId) {
        UserSession session = getSession(chatId);
        List<UserSession.PaymentMessageInfo> messages = session.getActivePaymentMessages();

        int expiredCount = 0;
        for (UserSession.PaymentMessageInfo message : messages) {
            if (message.isExpired()) {
                session.removePaymentMessage(message.getPaymentId());
                botExecutor.deleteMessage(chatId, message.getMessageId());
                expiredCount++;
                log.debug("⏰ Удалено истекшее платежное сообщение: paymentId={}",
                        message.getPaymentId());
            }
        }

        if (expiredCount > 0) {
            log.info("🧹 Удалено {} истекших платежных сообщений для chatId={}",
                    expiredCount, chatId);
        }
    }

    /**
     * Проверить и очистить истекшие сообщения для всех пользователей
     */
    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void cleanupAllExpiredPaymentMessages() {
        synchronized (userSessions) {
            int totalCleaned = 0;
            for (Long chatId : userSessions.keySet()) {
                try {
                    cleanupExpiredPaymentMessages(chatId);
                    totalCleaned++;
                } catch (Exception e) {
                    log.warn("Ошибка очистки платежных сообщений для {}: {}",
                            chatId, e.getMessage());
                }
            }

//            if (totalCleaned > 0) {
//                log.info("✅ Очищены истекшие платежные сообщения для {} пользователей",
//                        totalCleaned);
//            }
        }
    }
}