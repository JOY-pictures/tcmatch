package com.devlink.devlink.service;

import com.devlink.devlink.model.User;
import com.devlink.devlink.model.UserRole;
import com.devlink.devlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public User createUser(Long chatId, String username, String firstName, String lastName) {
        User user = User.builder()
                .chatId(chatId)
                .username(username)
                .firstname(firstName)
                .lastname(lastName)
                .role(UserRole.FREELANCER)
                .rating(0.0)
                .registeredAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);
        log.info("✅ Создан пользователь: {}", savedUser);
        return savedUser;
    }

    public Optional<User> findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public boolean userExists(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }
}
