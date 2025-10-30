package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);

    Optional<User> findByUsername(String username);

    boolean existsByChatId(Long chatId);

    boolean existsByUsername(String username);
}
