package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
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
public class CommonKeyboards {

    private final UserService userService;

    public InlineKeyboardMarkup getKeyboardForUser(Long chatId) {

        if (!userService.userExists(chatId)) {
            return createUnauthorizedUserKeyboard();
        } else if (!userService.hasFullAccess(chatId)) {
            UserRole.RegistrationStatus status = userService.getRegistrationStatus(chatId);
            return createRegistrationInProgressKeyboard(status, chatId);
        } else {
            return createToMainMenuKeyboard();
        }
    }

    public InlineKeyboardMarkup createRegistrationInProgressKeyboard(UserRole.RegistrationStatus status, Long chatId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        switch (status) {
            case NOT_REGISTERED:
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                row1.add(InlineKeyboardButton.builder()
                        .text("🚀 Начать регистрацию")
                        .callbackData("register:start")
                        .build());
                rows.add(row1);
                break;

            case REGISTERED:
                // 🔥 ДОБАВЛЯЕМ ЭТАП ВЫБОРА РОЛИ

                List<InlineKeyboardButton> roleRow1 = new ArrayList<>();
                roleRow1.add(InlineKeyboardButton.builder()
                        .text("👨‍💻 Я Исполнитель")
                        .callbackData("register:role:freelancer")
                        .build());

                List<InlineKeyboardButton> roleRow2 = new ArrayList<>();
                roleRow2.add(InlineKeyboardButton.builder()
                        .text("👔 Я Заказчик")
                        .callbackData("register:role:customer")
                        .build());

                rows.add(roleRow1);
                rows.add(roleRow2);
                break;

            case ROLE_SELECTED:
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                row2.add(InlineKeyboardButton.builder()
                        .text("📜 Ознакомиться с правилами")
                        .callbackData("rules:view")
                        .build());
                rows.add(row2);
                break;

            case RULES_VIEWED:
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                row3.add(InlineKeyboardButton.builder()
                        .text("✅ Принять правила")
                        .callbackData("rules:accept")
                        .build());
                rows.add(row3);

//                List<InlineKeyboardButton> row4 = new ArrayList<>();
//                row4.add(InlineKeyboardButton.builder()
//                        .text("📜 Перечитать правила")
//                        .callbackData("rules:view")
//                        .build());
//                rows.add(row4);
                break;
            case RULES_ACCEPTED:
                // Если статус RULES_ACCEPTED - возвращаем главное меню
                return createMainMenuKeyboard(chatId);
        }

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createUnauthorizedUserKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        row1.add(InlineKeyboardButton.builder()
                .text("Начать регистрацию")
                .callbackData("register:start")
                .build());
        rows.add(row1);

        inlineKeyboard.setKeyboard(rows);
        log.debug("✅ Unauthorized user keyboard created successfully");
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createToMainMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDFE0Главный экран")
                .callbackData("main:menu")
                .build());
        rows.add(row1);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createOneButtonKeyboard(String text, String callbackData) {
        // Создаем кнопку
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();

        // Создаем ряд с этой кнопкой
        List<InlineKeyboardButton> row = List.of(button);

        // Создаем разметку клавиатуры
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }

    public InlineKeyboardMarkup createPaginationKeyboardForContext(PaginationContext context) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> navRow = new ArrayList<>();

        // 🔥 ОПРЕДЕЛЯЕМ ТИП СУЩНОСТИ И ACTION_TYPE
        String actionType = getActionTypeByContext(context.contextKey()); // "project" или "application"
        String entityType = context.entityType(); // "PROJECT" или "APPLICATION"

        // 1. Кнопки пагинации
        if (context.hasPreviousPage()) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("◀️ Назад")
                    // 🔥 Формат: application:prev:my_applications
                    .callbackData(actionType + ":pagination:prev:" + context.contextKey() + ":" + entityType)
                    .build());
        }

        if (context.hasNextPage()) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("Вперед ▶️")
                    // 🔥 Формат: application:next:my_applications
                    .callbackData(actionType + ":pagination:next:" + context.contextKey() + ":" + entityType)
                    .build());
        }

        if (!navRow.isEmpty()) {
            keyboard.add(navRow);
        }

        // 2. Дополнительная строка: Возврат в меню
        List<InlineKeyboardButton> secondaryRow = new ArrayList<>();

        // Если контекст - это Отклики (Мои отклики или На мои проекты)
        if (context.contextKey().equals(PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY) ||
                context.contextKey().equals(PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY)) {

            // Кнопка ведет в главное меню откликов (или в общее меню, если нет отдельного меню откликов)
            secondaryRow.add(InlineKeyboardButton.builder()
                    .text("↩️ В меню откликов")
                    .callbackData("navigation:back") // Предполагаем, что такой роутер есть
                    .build());

        } else {
            // Если контекст - это Проекты (Поиск, Избранное, Мои проекты)

            // Кнопка ведет в меню проектов
            secondaryRow.add(InlineKeyboardButton.builder()
                    .text("↩️ Обратно")
                    .callbackData("navigation:back")
                    .build());
        }

        keyboard.add(secondaryRow);

        return new InlineKeyboardMarkup(keyboard);
    }


    private String getActionTypeByContext(String contextKey) {
        return switch (contextKey) {
            case PaginationContextKeys.PROJECT_FAVORITES_CONTEXT_KEY,
                 PaginationContextKeys.PROJECT_SEARCH_CONTEXT_KEY,
                 PaginationContextKeys.MY_PROJECTS_CONTEXT_KEY -> "project";
            case PaginationContextKeys.FREELANCER_APPLICATIONS_CONTEXT_KEY,
                 PaginationContextKeys.PROJECT_APPLICATIONS_CONTEXT_KEY -> "application";
            default -> "project"; // fallback
        };
    }

    public InlineKeyboardMarkup createMainMenuKeyboard(Long chatId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Мой профиль")
                .callbackData("user_profile:show")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("💼 Проекты")
                .callbackData("project:menu")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();

        row2.add(InlineKeyboardButton.builder()
                .text("🔔 Уведомления")
                .callbackData("notification:main") // 🔥 Новая команда!
                .build());

        row2.add(InlineKeyboardButton.builder()
                .text("ℹ️ Помощь")
                .callbackData("help:menu")
                .build());

        UserDto user = userService.getUserDtoByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        rows.add(row1);
        rows.add(row2);

        if (user.getRole().equals(UserRole.FREELANCER)) {
            List<InlineKeyboardButton> row3 = new ArrayList<>();
            row3.add(InlineKeyboardButton.builder()
                    .text("💰 Тарифы")
                    .callbackData("subscription:show_menu") // 🔥 Новая команда!
                    .build());
            rows.add(row3);
        }

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createBackButton() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back") // Универсальный callback для возврата
                .build());
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }
}
