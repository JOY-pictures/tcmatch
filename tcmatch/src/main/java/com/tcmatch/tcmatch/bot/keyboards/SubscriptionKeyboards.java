package com.tcmatch.tcmatch.bot.keyboards;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionKeyboards {
    public InlineKeyboardMarkup createSubscriptionKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> subscriptionRow = new ArrayList<>();
        subscriptionRow.add(InlineKeyboardButton.builder()
                .text("💎 Купить подписку")
                .callbackData("subscription:buy")
                .build());
        rows.add(subscriptionRow);

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("application:cancel")
                .build());
        rows.add(backRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }
}
