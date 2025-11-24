package com.tcmatch.tcmatch.bot.keyboards;

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
public class ProfileKeyboards {

    public InlineKeyboardMarkup createPersonalAccountKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📊 Статистика")
                .callbackData("user_profile:statistics")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("✏️ Профиль")
                .callbackData("user_profile:edit")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }
}
