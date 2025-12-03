package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 ID платежа, присвоенный ЮKassa. Используется для поиска по вебхуку.
    @Column(unique = true, nullable = false)
    private String paymentId;

    // ID пользователя
    @Column(nullable = false)
    private Long chatId;

    // Ключ для предотвращения двойной обработки запроса
    @Column(unique = true, nullable = false)
    private UUID idempotenceKey;

    // Тариф, который пользователь пытался купить
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;

    // Сумма, которая была заплачена
    @Column(nullable = false)
    private Double amount;

    // Текущий статус транзакции
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt; // Дата успешной обработки

    // Конструктор для создания новой PENDING транзакции
    public Transaction(String paymentId, Long chatId, UUID idempotenceKey, SubscriptionTier tier, Double amount) {
        this.paymentId = paymentId;
        this.chatId = chatId;
        this.idempotenceKey = idempotenceKey;
        this.tier = tier;
        this.amount = amount;
        this.status = TransactionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}