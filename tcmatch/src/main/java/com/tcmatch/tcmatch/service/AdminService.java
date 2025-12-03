package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.enums.AdminAccess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AdminService {

    @Value("#{'${admin.super-admin-ids}'.split(',')}")
    private List<Long> superAdminIds;

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
}