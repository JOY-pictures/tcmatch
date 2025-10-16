package com.devlink.devlink.service;

import com.devlink.devlink.model.RegistrationStatus;
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

    public User registerFromTelegram(Long chatId, String username, String firstName, String lastName) {
        Optional<User> existingUser = userRepository.findByChatId(chatId);

        if (existingUser.isPresent()) {
            log.info("✅ user already exists: {}", existingUser.get());
            return existingUser.get();
        }

        User user = User.builder()
                .chatId(chatId)
                .username(username)
                .firstname(firstName)
                .lastname(lastName)
                .role(UserRole.FREELANCER)
                .rating(0.0)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();
        User savedUser = userRepository.save(user);
        log.info("✅ Создан пользователь: {}", savedUser);
        return savedUser;
    }

    public User markRulesViewed(Long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRegistrationStatus(RegistrationStatus.RULES_VIEWED);
        user.setRulesViewedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("📜 User viewed rules: {}", chatId);
        return savedUser;
    }
    public User acceptRules(Long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRegistrationStatus(RegistrationStatus.RULES_ACCEPTED);
        user.setRulesAcceptedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("✅ User accepted rules: {}", chatId);
        return savedUser;
    }

    public boolean hasFullAccess(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(user -> user.getRegistrationStatus() == RegistrationStatus.RULES_ACCEPTED)
                .orElse(false);
    }

    public RegistrationStatus getRegistrationStatus(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getRegistrationStatus)
                .orElse(RegistrationStatus.NOT_REGISTERED);
    }

    public Optional<User> findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public boolean userExists(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }
}
