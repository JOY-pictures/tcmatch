package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.events.PaymentCompletedEvent;
import com.tcmatch.tcmatch.model.Transaction;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.model.enums.TransactionStatus;
import com.tcmatch.tcmatch.payment.yoomoney.YooMoneyClient;
import com.tcmatch.tcmatch.payment.yoomoney.dto.YooMoneyPaymentResponse;
import com.tcmatch.tcmatch.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionPaymentService {

    private final YooMoneyClient yooMoneyClient;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public PaymentInfo generatePaymentUrl(Long chatId, SubscriptionTier selectedTier, Double amountToPay) {
// 1. Создание ключа идемпотентности
        log.info("🔄 Начало генерации payment URL: chatId={}, tier={}, amount={}",
                chatId, selectedTier, amountToPay);

        try {
            UUID idempotenceKey = UUID.randomUUID();
            String description = String.format("Покупка тарифа %s для пользователя %d",
                    selectedTier.getDisplayName(), chatId);

            log.info("📤 Создание платежа в ЮKassa: description={}, idempotenceKey={}",
                    description, idempotenceKey);

            YooMoneyPaymentResponse response = yooMoneyClient.createPayment(
                    amountToPay,
                    description,
                    idempotenceKey
            );

            log.info("✅ Ответ от ЮKassa: paymentId={}, status={}",
                    response.getId(), response.getStatus());

            String paymentId = response.getId();

            // 🔥 КРИТИЧЕСКИЙ МОМЕНТ - СОХРАНЕНИЕ
            Transaction transaction = new Transaction(
                    response.getId(),
                    chatId,
                    idempotenceKey,
                    selectedTier,
                    amountToPay
            );

            Transaction saved = transactionRepository.save(transaction);
            log.info("💾 Транзакция сохранена в БД: id={}, paymentId={}, status={}",
                    saved.getId(), saved.getPaymentId(), saved.getStatus());

            log.info("🔗 Confirmation URL: {}", response.getConfirmation().getConfirmationUrl());

            // 3. 🔥 Возвращаем объект с paymentId и URL
            return new PaymentInfo(
                    paymentId,
                    response.getConfirmation().getConfirmationUrl()
            );
        } catch (Exception e) {
            log.error("❌ КРИТИЧЕСКАЯ ОШИБКА при создании платежа: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать ссылку для оплаты.", e);
        }
    }

    // =================================================================
    // 2. ОБРАБОТКА УВЕДОМЛЕНИЙ (ВЕБХУКОВ)
    // =================================================================

    /**
     * Обрабатывает уведомление о статусе платежа, полученное от ЮKassa (вебхук).
     */
    @Transactional
    public void handlePaymentNotification(String paymentId, String status) {
        log.info("🔍 Обработка вебхука: paymentId={}, status={}", paymentId, status);

        Transaction tx = transactionRepository.findByPaymentId(paymentId)
                .orElseGet(() -> {
                    log.error("❌ Транзакция не найдена: {}", paymentId);
                    return null;
                });

        if (tx == null) {
            log.warn("⚠️ Пропускаем вебхук - транзакция не найдена");
            return;
        }

        Long chatId = tx.getChatId();
        SubscriptionTier tier = tx.getTier();
        Double amount = tx.getAmount();

        log.info("💰 Найдена транзакция: chatId={}, tier={}, amount={}",
                chatId, tier, amount);

        if ("succeeded".equals(status)) {
            handleSuccessfulPayment(tx, chatId, paymentId, tier, amount);
        } else if ("canceled".equals(status)) {
            handleCanceledPayment(tx, chatId, paymentId, tier, amount);
        }
    }

    private void handleSuccessfulPayment(Transaction tx, Long chatId, String paymentId,
                                         SubscriptionTier tier, Double amount) {
        try {
            log.info("✅ Обработка успешного платежа: {}", paymentId);

            // Активация подписки
            subscriptionService.upgradeSubscription(chatId, tier);

            // Обновление транзакции
            tx.setStatus(TransactionStatus.SUCCEEDED);
            tx.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(tx);

            log.info("🎉 Подписка {} активирована для {}", tier, chatId);

            // 🔥 ПУБЛИКАЦИЯ СОБЫТИЯ
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    this,
                    chatId,
                    paymentId,
                    tier,
                    true,
                    "Платеж успешно завершен",
                    amount
            ));

        } catch (Exception e) {
            log.error("❌ Ошибка активации подписки: {}", e.getMessage(), e);

            // 🔥 ПУБЛИКАЦИЯ СОБЫТИЯ ОБ ОШИБКЕ
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    this,
                    chatId,
                    paymentId,
                    tier,
                    false,
                    "Ошибка активации подписки: " + e.getMessage(),
                    amount
            ));
        }
    }

    private void handleCanceledPayment(Transaction tx, Long chatId, String paymentId,
                                       SubscriptionTier tier, Double amount) {
        log.info("❌ Обработка отмененного платежа: {}", paymentId);

        // Обновление транзакции
        tx.setStatus(TransactionStatus.CANCELED);
        tx.setProcessedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        log.warn("💸 Платеж {} отменен", paymentId);

        // 🔥 ПУБЛИКАЦИЯ СОБЫТИЯ
        eventPublisher.publishEvent(new PaymentCompletedEvent(
                this,
                chatId,
                paymentId,
                tier,
                false,
                "Платеж отменен пользователем",
                amount
        ));
    }

    /**
     * 🔥 DTO для возврата информации о платеже
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentInfo {
        private String paymentId;
        private String paymentUrl;
    }
}
