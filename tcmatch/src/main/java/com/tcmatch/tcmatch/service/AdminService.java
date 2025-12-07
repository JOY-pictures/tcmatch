package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.enums.AdminAccess;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminService {

    @Value("${admin.super-admin-ids:123456789}")
    private String adminIdsConfig;

    private List<Long> superAdminIds;

    @PostConstruct
    public void init() {
        // Разбираем строку с ID админов через запятую
        if (adminIdsConfig != null && !adminIdsConfig.trim().isEmpty()) {
            this.superAdminIds = Arrays.stream(adminIdsConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            log.info("✅ Загружены ID админов: {}", superAdminIds);
        } else {
            this.superAdminIds = List.of();
            log.warn("⚠️ Список админов пуст или не настроен");
        }
    }

    /**
     * Проверяет, является ли пользователь админом
     */
    public boolean isAdmin(Long chatId) {
        return superAdminIds.contains(chatId);
    }

    /**
     * Проверяет, является ли пользователь супер-админом
     */
    public boolean isSuperAdmin(Long chatId) {
        return superAdminIds.contains(chatId) &&
                superAdminIds.indexOf(chatId) == 0; // Первый в списке = супер-админ
    }

    /**
     * Получает уровень доступа админа
     */
    public AdminAccess getAdminAccess(Long chatId) {
        if (!isAdmin(chatId)) {
            return null;
        }
        // Пока всем админам даем полный доступ
        // Позже можно будет разграничивать
        return AdminAccess.FULL;
    }

    public List<Long> getAllAdminChatIds() {
        return superAdminIds;
    }
}