package com.tcmatch.tcmatch.service.notifications;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.keyboards.SubscriptionKeyboards;
import com.tcmatch.tcmatch.events.PaymentCompletedEvent;
import com.tcmatch.tcmatch.model.UserSession;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentObserverService {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final SubscriptionKeyboards subscriptionKeyboards;
    private final NotificationService notificationService; // 🔥 Добавили

    /**
     * 🔥 Отправка сообщения с кнопкой оплаты
     */
    public Integer sendPaymentLinkMessage(Long chatId, String paymentUrl,
                                          SubscriptionTier tier, String paymentId) {

        String paymentText = String.format("""
            💰 <b>ССЫЛКА ДЛЯ ОПЛАТЫ</b>
            
            <blockquote>Тариф: <b>%s</b>
            Сумма: <b>%.0f ₽</b>
            
            Нажмите кнопку ниже для оплаты через ЮKassa.</blockquote>
            
            ⏱️ <i>Ссылка действительна 15 минут</i>
            """,
                tier.getDisplayName(),
                tier.getPrice()
        );

        InlineKeyboardMarkup keyboard = subscriptionKeyboards.createPaymentLinkKeyboard(paymentUrl);

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
            
            ✅ Тариф: <b>%s</b> активирован.
            💰 Сумма: <b>%.0f ₽</b>
            📋 ID платежа: <code>%s</code>
            
            Спасибо за покупку! 🚀</blockquote>
            """,
                event.getTier().getDisplayName(),
                event.getAmount(),
                event.getPaymentId().substring(0, 8)
        );

        String callbackData = String.format("payment:details:%s", event.getPaymentId());

        // 🔥 ИСПОЛЬЗУЕМ CENTRAL NOTIFICATION SERVICE
        notificationService.createNotification(chatId, text, callbackData);

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

        String callbackData = String.format("payment:retry:%s", event.getPaymentId());

        // 🔥 ИСПОЛЬЗУЕМ CENTRAL NOTIFICATION SERVICE
        notificationService.createNotification(chatId, text, callbackData);

        log.info("📨 Уведомление об отмене платежа создано через NotificationService");
    }
}