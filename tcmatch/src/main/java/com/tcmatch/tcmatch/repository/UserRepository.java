package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);

    boolean existsByUserName(String userName);

    boolean existsByChatId(Long chatId);

    // 🔥 МЕТОД ДЛЯ ПАКЕТНОЙ ЗАГРУЗКИ ПО CHAT_ID
    List<User> findByChatIdIn(List<Long> chatIds);


}
