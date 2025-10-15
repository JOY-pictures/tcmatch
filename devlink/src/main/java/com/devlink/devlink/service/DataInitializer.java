package com.devlink.devlink.service;

import com.devlink.devlink.model.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserService userService;

    @PostConstruct
    public void initTestData() {
        // Создаем тестового пользователя при запуске
        if (!userService.userExists(123456789L)) {
            User testUser = userService.createUser(
                    123456789L,
                    "testuser",
                    "Test",
                    "User"
            );
            log.info("🎯 Тестовый пользователь создан: {}", testUser);
        } else {
            log.info("🎯 Тестовый пользователь уже существует");
        }
    }
}
