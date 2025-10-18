package com.devlink.devlink.bot.keyboards;


import com.devlink.devlink.model.RegistrationStatus;
import com.devlink.devlink.service.UserService;
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
public class KeyboardFactory {

    private final UserService userService;

    public InlineKeyboardMarkup getKeyboardForUser(Long chatId) {

        if (!userService.userExists(chatId)) {
            return createUnauthorizedUserKeyboard();
        } else if (!userService.hasFullAccess(chatId)) {
            RegistrationStatus status = userService.getRegistrationStatus(chatId);
            return createRegistrationInProgressKeyboard(status);
        } else {
            return createMainMenuKeyboard();
        }
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

        List<InlineKeyboardButton> row2 = new ArrayList<>();

        row2.add(InlineKeyboardButton.builder()
                .text("📋 Правила платформы")
                .callbackData("rules:preview")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("ℹ️ О проекте")
                .callbackData("menu:about")
                .build());
        rows.add(row2);
        inlineKeyboard.setKeyboard(rows);
        log.debug("✅ Unauthorized user keyboard created successfully");
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createRegistrationInProgressKeyboard(RegistrationStatus status) {
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

                List<InlineKeyboardButton> row4 = new ArrayList<>();
                row4.add(InlineKeyboardButton.builder()
                        .text("📜 Перечитать правила")
                        .callbackData("rules:view")
                        .build());
                rows.add(row4);
                break;
            case RULES_ACCEPTED:
                // Если статус RULES_ACCEPTED - возвращаем главное меню
                return createMainMenuKeyboard();
        }

        // Кнопка помощи для всех статусов
        List<InlineKeyboardButton> helpRow = new ArrayList<>();
        helpRow.add(InlineKeyboardButton.builder()
                .text("❓ Помощь")
                .callbackData("menu:help")
                .build());
        rows.add(helpRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
        }

    public InlineKeyboardMarkup createMainMenuKeyboard() {
        System.out.println("ha");
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Мой профиль")
                .callbackData("menu:profile")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("💼 Проекты")
                .callbackData("menu:projects")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🚀 Создать проект")
                .callbackData("menu:create_project")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("👥 Найти исполнителей")
                .callbackData("menu:browse_freelancers")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("📊 Мои заказы")
                .callbackData("menu:my_orders")
                .build());
        row3.add(InlineKeyboardButton.builder()
                .text("ℹ️ Помощь")
                .callbackData("menu:help")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }


}
