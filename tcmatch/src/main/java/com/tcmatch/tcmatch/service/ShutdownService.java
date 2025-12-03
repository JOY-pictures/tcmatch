package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShutdownService implements ApplicationListener<ContextClosedEvent> {

    private final UserSessionService userSessionService;
    private final BotExecutor botExecutor;
    private final ExecutorService cleanupExecutor = Executors.newFixedThreadPool(10);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("🚨 Получен сигнал завершения работы приложения");
        cleanupAllUserMessages();
        shutdownExecutor();
    }

    /**
     * 🔥 Очистка всех сообщений всех пользователей
     */
    @Transactional
    public void cleanupAllUserMessages() {
        try {
            log.info("🧹 Начинаем очистку сообщений всех пользователей...");

            // 1. Получаем все сессии
            Map<Long, UserSession> allSessions = userSessionService.getAllSessions();
            log.info("📊 Всего активных сессий: {}", allSessions.size());

            if (allSessions.isEmpty()) {
                log.info("✅ Нет активных сессий для очистки");
                return;
            }

            int totalDeleted = 0;
            int totalFailed = 0;

            // 2. Для каждой сессии запускаем асинхронную очистку
            List<CompletableFuture<Void>> cleanupFutures = allSessions.entrySet().stream()
                    .map(entry -> CompletableFuture.runAsync(() ->
                            cleanupUserSession(entry.getKey(), entry.getValue()), cleanupExecutor))
                    .toList();

            // 3. Ждем завершения всех задач
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    cleanupFutures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(30, TimeUnit.SECONDS); // Таймаут 30 секунд
                log.info("✅ Асинхронная очистка завершена");
            } catch (Exception e) {
                log.warn("⚠️ Таймаут при очистке: {}", e.getMessage());
            }

            log.info("🎯 Итоги очистки: удалено={}, ошибок={}", totalDeleted, totalFailed);

        } catch (Exception e) {
            log.error("❌ Критическая ошибка при очистке сообщений: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 Очистка сообщений конкретного пользователя
     */
    @Async
    public CompletableFuture<Void> cleanupUserSession(Long chatId, UserSession session) {
        log.debug("🧹 Очистка сообщений для пользователя: {}", chatId);

        try {
            int deletedCount = 0;

            // 1. Удаляем главное сообщение
            Integer mainMessageId = session.getMainMessageId();
            if (mainMessageId != null) {
                try {
                    botExecutor.deleteMessage(chatId, mainMessageId);
                    deletedCount++;
                    log.debug("🗑️ Удалено главное сообщение: {}", mainMessageId);
                } catch (Exception e) {
                    log.debug("⚠️ Главное сообщение уже удалено: {}", mainMessageId);
                }
            }

            // 2. Удаляем временные сообщения
            List<Integer> temporaryMessageIds = session.getTemporaryMessageIds();
            if (!temporaryMessageIds.isEmpty()) {
                for (Integer messageId : temporaryMessageIds) {
                    try {
                        botExecutor.deleteMessage(chatId, messageId);
                        deletedCount++;
                    } catch (Exception e) {
                        // Сообщение уже удалено
                    }
                }
                log.debug("🗑️ Удалено временных сообщений: {}", temporaryMessageIds.size());
            }

            // 3. Удаляем платежные сообщения
            List<UserSession.PaymentMessageInfo> paymentMessages = session.getActivePaymentMessages();
            if (!paymentMessages.isEmpty()) {
                for (UserSession.PaymentMessageInfo paymentMessage : paymentMessages) {
                    try {
                        botExecutor.deleteMessage(chatId, paymentMessage.getMessageId());
                        deletedCount++;
                    } catch (Exception e) {
                        // Сообщение уже удалено
                    }
                }
                log.debug("🗑️ Удалено платежных сообщений: {}", paymentMessages.size());
            }

            // 4. Удаляем push-сообщение
            Integer pushMessageId = session.getLastPushMessageId();
            if (pushMessageId != null) {
                try {
                    botExecutor.deleteMessage(chatId, pushMessageId);
                    deletedCount++;
                    log.debug("🗑️ Удалено push-сообщение: {}", pushMessageId);
                } catch (Exception e) {
                    log.debug("⚠️ Push-сообщение уже удалено: {}", pushMessageId);
                }
            }

            if (deletedCount > 0) {
                log.info("✅ Удалено {} сообщений для пользователя {}", deletedCount, chatId);
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ Ошибка очистки сообщений для {}: {}", chatId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 🔥 Завершение работы ExecutorService
     */
    private void shutdownExecutor() {
        try {
            log.info("🛑 Завершаем работу cleanup executor...");
            cleanupExecutor.shutdown();

            if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("⚠️ cleanup executor не завершился вовремя, принудительное завершение");
                cleanupExecutor.shutdownNow();
            }

            log.info("✅ cleanup executor завершен");
        } catch (InterruptedException e) {
            log.error("❌ Ошибка завершения executor: {}", e.getMessage());
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 🔥 Принудительная очистка для конкретного пользователя (можно вызывать из бота)
     */
    public void cleanupUserMessages(Long chatId) {
        try {
            UserSession session = userSessionService.getSession(chatId);
            cleanupUserSession(chatId, session).get(5, TimeUnit.SECONDS);
            log.info("✅ Принудительная очистка завершена для {}", chatId);
        } catch (Exception e) {
            log.error("❌ Ошибка принудительной очистки для {}: {}", chatId, e.getMessage());
        }
    }
}