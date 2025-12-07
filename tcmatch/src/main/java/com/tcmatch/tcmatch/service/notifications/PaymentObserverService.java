package com.tcmatch.tcmatch.service.notifications;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.keyboards.SubscriptionKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.WalletKeyboards;
import com.tcmatch.tcmatch.events.PaymentCompletedEvent;
import com.tcmatch.tcmatch.model.UserSession;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentObserverService {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final WalletKeyboards walletKeyboards;
    private final NotificationService notificationService; // 🔥 Добавили
    private final WalletService walletService; // Добавляем для получения баланса


    /**
     * 🔥 Отправка сообщения с кнопкой оплаты
     */
    public Integer sendPaymentLinkMessage(Long chatId, String paymentUrl, BigDecimal amount, String paymentId) {


        String paymentText = String.format("""
            💰 <b>Пополнение баланса</b>
            
            Сумма пополнения: <b>%s ₽</b>
            
            Нажмите кнопку ниже для оплаты через ЮKassa.
            
            ⏱️ <i>Ссылка действительна 15 минут</i>
            
            <code>ID платежа: %s</code>
            """,
                formatAmount(amount),
                paymentId.substring(0, Math.min(paymentId.length(), 8))
        );

        InlineKeyboardMarkup keyboard = walletKeyboards.createPaymentLinkKeyboard(paymentUrl);

        // Отправляем сообщение
        Integer messageId = botExecutor.sendHtmlMessageReturnId(chatId, paymentText, keyboard);

        if (messageId != null) {
            // 🔥 Сохраняем в UserSession отдельно от temporaryMessages
            userSessionService.addPaymentMessage(chatId, paymentId, messageId);
            log.info("💳 Платежное сообщение сохранено в сессии: chatId={}, paymentId={}, messageId={}",
                    chatId, paymentId, messageId);
        }

        return messageId;
    }

    /**
     * 🔥 Удаление платежного сообщения
     */
    public void deletePaymentMessage(Long chatId, String paymentId) {
        Optional<UserSession.PaymentMessageInfo> paymentMessageOpt =
                userSessionService.findPaymentMessage(chatId, paymentId);

        if (paymentMessageOpt.isPresent()) {
            UserSession.PaymentMessageInfo paymentMessage = paymentMessageOpt.get();

            try {
                // Удаляем сообщение из Telegram
                botExecutor.deleteMessage(chatId, paymentMessage.getMessageId());

                // Удаляем из сессии
                userSessionService.removePaymentMessage(chatId, paymentId);

                log.info("🗑️ Удалено платежное сообщение: chatId={}, paymentId={}, messageId={}",
                        chatId, paymentId, paymentMessage.getMessageId());

            } catch (Exception e) {
                log.warn("⚠️ Не удалось удалить платежное сообщение: {}", e.getMessage());
                // Все равно удаляем из сессии, чтобы не копить
                userSessionService.removePaymentMessage(chatId, paymentId);
            }
        }
    }

    /**
     * 🔥 Очистить все платежные сообщения пользователя
     */
    public void cleanupUserPaymentMessages(Long chatId) {
        List<Integer> messageIds = userSessionService.getPaymentMessageIds(chatId);

        if (!messageIds.isEmpty()) {
            log.info("🧹 Очистка {} платежных сообщений для chatId={}",
                    messageIds.size(), chatId);

            // Удаляем все сообщения из Telegram
            for (Integer messageId : messageIds) {
                try {
                    botExecutor.deleteMessage(chatId, messageId);
                } catch (Exception e) {
                    log.debug("Сообщение уже удалено: {}", messageId);
                }
            }

            // Очищаем сессию
            userSessionService.clearPaymentMessages(chatId);
        }
    }

    /**
     * 🔥 Обработка успешной оплаты
     */
    @Async
    @EventListener
    public void handlePaymentSuccess(PaymentCompletedEvent event) {
        if (!event.isSuccess()) return;

        Long chatId = event.getChatId();
        String paymentId = event.getPaymentId();

        try {
            // 1. Удаляем платежное сообщение
            deletePaymentMessage(chatId, paymentId);

            // 2. 🔥 СОЗДАЕМ УВЕДОМЛЕНИЕ ЧЕРЕЗ NotificationService
            createPaymentSuccessNotification(chatId, event);

            // 3. Очищаем все остальные платежные сообщения (если есть)
            userSessionService.clearPaymentMessages(chatId);

            log.info("🎉 Обработка успешного платежа завершена: chatId={}, paymentId={}",
                    chatId, paymentId);

        } catch (Exception e) {
            log.error("❌ Ошибка обработки успешного платежа: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 Обработка отмененной оплаты
     */
    @Async
    @EventListener
    public void handlePaymentCanceled(PaymentCompletedEvent event) {
        if (event.isSuccess()) return;

        Long chatId = event.getChatId();
        String paymentId = event.getPaymentId();

        try {
            // 1. Удаляем платежное сообщение
            deletePaymentMessage(chatId, paymentId);

            // 2. 🔥 СОЗДАЕМ УВЕДОМЛЕНИЕ ОБ ОТМЕНЕ
            createPaymentCancelledNotification(chatId, event);

            log.info("📝 Обработка отмененного платежа завершена: chatId={}, paymentId={}",
                    chatId, paymentId);

        } catch (Exception e) {
            log.error("❌ Ошибка обработки отмененного платежа: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 Создание уведомления об успешной оплате через NotificationService
     */
    @Async
    private void createPaymentSuccessNotification(Long chatId, PaymentCompletedEvent event) {
        String text = String.format("""
            <blockquote>
            🎉 <b>ОПЛАТА УСПЕШНА!</b>
            
            💰 Вы пополнили баланс на сумму: <b>%.0f ₽</b>
            
            📋 ID платежа: <code>%s</code>
            
            Спасибо за покупку! 🚀</blockquote>
            """,
                event.getAmount(),
                event.getPaymentId().substring(0, 8)
        );

        // 🔥 ИСПОЛЬЗУЕМ CENTRAL NOTIFICATION SERVICE
        notificationService.createNotification(chatId, text, "");

        log.info("📨 Уведомление об успешной оплате создано через NotificationService");
    }

    /**
     * 🔥 Создание уведомления об отмене через NotificationService
     */
    @Async
    private void createPaymentCancelledNotification(Long chatId, PaymentCompletedEvent event) {
        String text = String.format("""
            ❌ <b>ПЛАТЕЖ ОТМЕНЕН</b>
            
            Платеж %s был отменен.
            
            %s
            
            Вы можете попробовать оплатить снова.
            """,
                event.getPaymentId().substring(0, 8) + "...",
                event.getMessage()
        );

        // 🔥 ИСПОЛЬЗУЕМ CENTRAL NOTIFICATION SERVICE
        notificationService.createNotification(chatId, text, null);

        log.info("📨 Уведомление об отмене платежа создано через NotificationService");
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}