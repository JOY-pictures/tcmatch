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
public class BalancePaymentService {

    private final YooMoneyClient yooMoneyClient;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionRepository transactionRepository;
    // 🔥 Инжектируем наш сервис кошелька для пополнения баланса
    private final WalletService walletService;

    @Transactional
    public PaymentInfo generatePaymentUrl(Long chatId, BigDecimal amountToPay) {
        log.info("🔄 Начало генерации payment URL для пополнения баланса: chatId={}, amount={}",
                chatId, amountToPay);

        // Использование BigDecimal для расчетов в YooKassa тоже, если API это поддерживает
        Double amountForYooKassa = amountToPay.doubleValue();

        try {
            UUID idempotenceKey = UUID.randomUUID();
            String description = String.format("Пополнение баланса (Chat ID: %d) на сумму %s RUB",
                    chatId, amountToPay); // Описание теперь о пополнении

            log.info("📤 Создание платежа в ЮKassa: description={}, idempotenceKey={}",
                    description, idempotenceKey);

            YooMoneyPaymentResponse response = yooMoneyClient.createPayment(
                    amountForYooKassa, // Используем double, как у вас было
                    description,
                    idempotenceKey
            );

            log.info("✅ Ответ от ЮKassa: paymentId={}, status={}",
                    response.getId(), response.getStatus());

            // 🔥 КРИТИЧЕСКИЙ МОМЕНТ - СОХРАНЕНИЕ
            Transaction transaction = new Transaction(
                    response.getId(),
                    chatId,
                    idempotenceKey,
                    amountForYooKassa
            );

            Transaction saved = transactionRepository.save(transaction);
            log.info("💾 Транзакция сохранена в БД: id={}, paymentId={}, status={}",
                    saved.getId(), saved.getPaymentId(), saved.getStatus());

            log.info("🔗 Confirmation URL: {}", response.getConfirmation().getConfirmationUrl());

            return new PaymentInfo(
                    response.getId(),
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
        // SubscriptionTier tier = tx.getTier(); // Больше не используется
        Double amount = tx.getAmount();

        log.info("💰 Найдена транзакция: chatId={}, amount={}", chatId, amount);

        if ("succeeded".equals(status)) {
            handleSuccessfulPayment(tx, chatId, paymentId, amount);
        } else if ("canceled".equals(status)) {
            handleCanceledPayment(tx, chatId, paymentId, amount);
        }
    }

    // 🔥 ИЗМЕНЕН: Удалена привязка к подписке. Теперь вызывается WalletService.deposit()
    private void handleSuccessfulPayment(Transaction tx, Long chatId, String paymentId, Double amount) {
        try {
            log.info("✅ Обработка успешного платежа для пополнения: {}", paymentId);

            // 🔥 ГЛАВНОЕ ИЗМЕНЕНИЕ: Пополняем баланс пользователя
            walletService.deposit(chatId, new BigDecimal(String.valueOf(amount)));

            // Обновление транзакции
            tx.setStatus(TransactionStatus.SUCCEEDED);
            tx.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(tx);

            log.info("🎉 Баланс пользователя {} успешно пополнен на {}", chatId, amount);

            // 🔥 ПУБЛИКАЦИЯ СОБЫТИЯ
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    this,
                    chatId,
                    paymentId,
                    true,
                    "Баланс успешно пополнен",
                    amount
            ));

        } catch (Exception e) {
            log.error("❌ Ошибка при пополнении баланса: {}", e.getMessage(), e);

            // 🔥 ПУБЛИКАЦИЯ СОБЫТИЯ ОБ ОШИБКЕ
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    this,
                    chatId,
                    paymentId,
                    false,
                    "Ошибка пополнения баланса: " + e.getMessage(),
                    amount
            ));
        }
    }

    private void handleCanceledPayment(Transaction tx, Long chatId, String paymentId, Double amount) {
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
