package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RegistrationKeyboard {

    @Lazy
    private final CommonKeyboards commonKeyboards;

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
                return commonKeyboards.createMainMenuKeyboard(chatId);
        }

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }
}
