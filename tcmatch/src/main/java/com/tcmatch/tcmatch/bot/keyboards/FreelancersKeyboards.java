package com.tcmatch.tcmatch.bot.keyboards;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FreelancersKeyboards {
    public InlineKeyboardMarkup createFreelancersMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();


        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск")
                .callbackData("freelancers:search")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("⭐ Избранные")
                .callbackData("freelancers:favorites")
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
