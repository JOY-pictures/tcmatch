package com.tcmatch.tcmatch.bot.keyboards;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WalletKeyboards {
    /**
     * 3. Клавиатура с кнопкой-ссылкой для оплаты через ЮKassa.
     * @param paymentUrl Ссылка, сгенерированная YooMoneyClient.
     */
    public InlineKeyboardMarkup createPaymentLinkKeyboard(String paymentUrl) {
        InlineKeyboardButton payButton = InlineKeyboardButton.builder()
                .text("💳 Оплатить через ЮKassa")
                .url(paymentUrl)
                .build();

        return new InlineKeyboardMarkup(List.of(
                List.of(payButton)
        ));
    }
}
