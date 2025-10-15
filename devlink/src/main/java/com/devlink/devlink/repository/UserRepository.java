package com.devlink.devlink.repository;

import com.devlink.devlink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);

    Optional<User> findByUsername(String username);

    boolean existsByChatId(Long chatId);

    boolean existsByUsername(String username);
}
