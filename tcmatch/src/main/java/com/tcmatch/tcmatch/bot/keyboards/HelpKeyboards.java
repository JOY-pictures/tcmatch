package com.tcmatch.tcmatch.bot.keyboards;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HelpKeyboards {
    public InlineKeyboardMarkup createHelpMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📜 Правила")
                .callbackData("help:rules")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("ℹ️ Информация")
                .callbackData("help:info")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🛠️ Тех поддержка")
                .callbackData("help:support")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }
}
