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

//            // На будущее: статистика, управление пользователями и т.д.
//            row2.add(InlineKeyboardButton.builder()
//                    .text("⚙️ Настройки")
//                    .callbackData("admin:settings")
//                    .build());
//
//            row2.add(InlineKeyboardButton.builder()
//                    .text("📊 Статистика")
//                    .callbackData("admin:stats")
//                    .build());
//
//            rows.add(row2);
        }

        // ========== РЯД 3: НАВИГАЦИЯ ==========

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("🔙 В главное меню")
                .callbackData("main:menu")
                .build());

//        row3.add(InlineKeyboardButton.builder()
//                .text("🔄 Обновить")
//                .callbackData("admin:refresh")
//                .build());

        rows.add(row3);

        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 🔥 БЫСТРАЯ КЛАВИАТУРА ДЛЯ УВЕДОМЛЕНИЙ
     * (отправляется сразу с уведомлением о новой заявке)
     */
    public InlineKeyboardMarkup createQuickActionKeyboard(Long requestId, Long userChatId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Основные быстрые действия
        List<InlineKeyboardButton> quickActions = new ArrayList<>();
        quickActions.add(InlineKeyboardButton.builder()
                .text("✅ Одобрить")
                .callbackData("admin:verification:quick_approve:" + requestId)
                .build());
        quickActions.add(InlineKeyboardButton.builder()
                .text("❌ Отклонить")
                .callbackData("admin:verification:quick_reject:" + requestId)
                .build());

        rows.add(quickActions);

        // Дополнительные действия
        List<InlineKeyboardButton> moreActions = new ArrayList<>();
        moreActions.add(InlineKeyboardButton.builder()
                .text("📋 Подробнее")
                .callbackData("admin:verification:details:" + requestId)
                .build());
        moreActions.add(InlineKeyboardButton.builder()
                .text("👤 Профиль")
                .callbackData("admin:user:view:" + userChatId)
                .build());

        rows.add(moreActions);

        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 🔥 Клавиатура "Готово" после действия
     */
    public InlineKeyboardMarkup createDoneKeyboard(Long requestId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text("✅ Обработано")
                                        .callbackData("admin:verification:done:" + requestId)
                                        .build()
                        )
                ))
                .build();
    }

    /**
     * 🔥 Клавиатура для ожидания комментария
     */
    public InlineKeyboardMarkup createAwaitCommentKeyboard(Long requestId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                InlineKeyboardButton.builder()
                                        .text("🚫 Отмена")
                                        .callbackData("admin:verification:cancel_reject:" + requestId)
                                        .build()
                        )
                ))
                .build();
    }
}