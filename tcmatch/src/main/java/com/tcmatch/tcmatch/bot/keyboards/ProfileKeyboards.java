package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.VerificationService;
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

    private final UserService userService;

    public InlineKeyboardMarkup createPersonalAccountKeyboard(Long chatId) {
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


        UserDto user = userService.getUserDtoByChatId(chatId).orElseThrow(()-> new RuntimeException("Пользователь не найден"));
        // 🔥 НОВАЯ КНОПКА ВЕРИФИКАЦИИ
        if (user.getRole() == UserRole.FREELANCER) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            row2.add(InlineKeyboardButton.builder()
                    .text("✅ Верификация")
                    .callbackData("verification:show")
                    .build());
            rows.add(row2);
        }


        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);

        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }
}
