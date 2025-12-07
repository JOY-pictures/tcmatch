package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Находит кошелек по ID пользователя.
     */
    Optional<Wallet> findByUserChatId(Long userChatId);
}