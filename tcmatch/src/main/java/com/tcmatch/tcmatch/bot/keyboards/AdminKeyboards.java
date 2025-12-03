package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.enums.AdminAccess;
import com.tcmatch.tcmatch.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminKeyboards {

    private final AdminService adminService;

    /**
     * Создает динамическое главное меню для админа
     * Меню меняется в зависимости от:
     * 1. Уровня доступа админа
     * 2. Наличия новых заявок
     * 3. Статуса системы
     */
    public InlineKeyboardMarkup createMainAdminMenu(Long chatId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ПРОВЕРЯЕМ ДОСТУП АДМИНА
        AdminAccess access = adminService.getAdminAccess(chatId);

        if (access == null) {
            log.error("Попытка доступа к админ-меню не-админом: {}", chatId);
            return null;
        }

        // ========== РЯД 1: ОСНОВНЫЕ ФУНКЦИИ ==========

        // 🔥 КНОПКА "ПРОВЕРКА ЗАЯВОК" (всегда есть у админов)
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        // TODO: Получить количество новых заявок (пока заглушка)
        int newRequestsCount = 5; // Будем получать из сервиса

        String verificationText = newRequestsCount > 0
                ? String.format("✅ Проверка заявок (%d)", newRequestsCount)
                : "✅ Проверка заявок";

        row1.add(InlineKeyboardButton.builder()
                .text(verificationText)
                .callbackData("admin:verification:list")
                .build());

        rows.add(row1);

        // ========== РЯД 2: ДОПОЛНИТЕЛЬНЫЕ ФУНКЦИИ (для супер-админа) ==========

        if (adminService.isSuperAdmin(chatId)) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();

            // На будущее: статистика, управление пользователями и т.д.
            row2.add(InlineKeyboardButton.builder()
                    .text("⚙️ Настройки")
                    .callbackData("admin:settings")
                    .build());

            row2.add(InlineKeyboardButton.builder()
                    .text("📊 Статистика")
                    .callbackData("admin:stats")
                    .build());

            rows.add(row2);
        }

        // ========== РЯД 3: НАВИГАЦИЯ ==========

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("🔙 В главное меню")
                .callbackData("main:menu")
                .build());

        row3.add(InlineKeyboardButton.builder()
                .text("🔄 Обновить")
                .callbackData("admin:refresh")
                .build());

        rows.add(row3);

        return new InlineKeyboardMarkup(rows);
    }
}