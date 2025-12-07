package com.tcmatch.tcmatch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Сущность Кошелька пользователя.
 * Хранит доступный баланс и замороженные средства.
 * Все операции с балансом должны производиться через WalletService.
 */
@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_chatId", nullable = false, precision = 19, scale = 4)
    private Long userChatId; // Ссылка на владельца кошелька

    // Текущий доступный баланс (для покупок и вывода)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    // Замороженные средства (для безопасных сделок, Escrow)
    @Column(name = "frozen_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal frozenBalance = BigDecimal.ZERO;

    public Wallet(Long userChatId) {
        this.userChatId = userChatId;
    }
}