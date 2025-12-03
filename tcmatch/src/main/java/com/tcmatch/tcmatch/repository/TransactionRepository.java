package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Ищем транзакцию по ID платежа ЮKassa для обработки вебхука
    Optional<Transaction> findByPaymentId(String paymentId);
}