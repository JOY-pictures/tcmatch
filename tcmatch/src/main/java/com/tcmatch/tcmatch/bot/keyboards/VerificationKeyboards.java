package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class VerificationKeyboards {

    private final VerificationService verificationService;

    public InlineKeyboardMarkup createMenuKeyboard(VerificationStatus status, Long chatId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();


        if (status == null || verificationService.canSendRequest(chatId)) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(InlineKeyboardButton.builder()
                        .text("✅ Верифицировать GitGub")
                        .callbackData("verification:start_github")
                        .build());
                rows.add(row);
        }

        // Кнопка ведет в меню проектов
        rows.add(List.of(InlineKeyboardButton.builder()
                .text("↩️ Обратно")
                .callbackData("navigation:back")
                .build()));
        keyboard.setKeyboard(rows);
        return keyboard;
    }
}
