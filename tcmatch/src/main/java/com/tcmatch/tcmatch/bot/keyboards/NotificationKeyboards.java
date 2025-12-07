package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationKeyboards {

    @Lazy
    @Autowired
    private NotificationService notificationService;

    public InlineKeyboardMarkup createGoToNotificationCenterKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InlineKeyboardButton.builder()
                .text("🔔 Центр уведомлений")
                .callbackData("notification:main") // 🔥 Новая команда!
                .build()));

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    /**
     * Создает клавиатуру действий для одной карточки уведомления.
     */
    public InlineKeyboardMarkup createNotificationItemKeyboard(Long notificationId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row = new ArrayList<>();



        // Кнопка "Посмотреть" (callback: notification:view:ID)
        if (notificationService.hasCallback(notificationId))
            row.add(InlineKeyboardButton.builder()
                    .text("👁️ Посмотреть")
                    .callbackData("notification:view:" + notificationId)
                    .build());

        // Кнопка "Удалить" (callback: notification:delete:ID)
        row.add(InlineKeyboardButton.builder()
                .text("🗑️ Удалить")
                .callbackData("notification:delete:" + notificationId)
                .build());

        inlineKeyboard.setKeyboard(List.of(row));
        return inlineKeyboard;
    }
}
