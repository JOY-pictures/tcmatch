package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    public InlineKeyboardMarkup createSubscriptionListKeyboard(List<SubscriptionTier> plans) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (SubscriptionTier tier : plans) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(String.format("%s (%.0f ₽/мес)", tier.getDisplayName(), tier.getPrice()))
                    .callbackData("subscription:select:" + tier.name())
                    .build();
            rows.add(List.of(button));
        }

        // Кнопка "Назад"
        rows.add(List.of(
                InlineKeyboardButton.builder()
                        .text("⬅️ Назад")
                        .callbackData("navigation:back")
                        .build()
        ));

        return new InlineKeyboardMarkup(rows);
    }


    /**
     * Клавиатура меню подписки.
     */
    public InlineKeyboardMarkup createSubscriptionMenuKeyboard(String currentTariffDisplay) {
        boolean isFree = currentTariffDisplay.contains("Бесплатно");
        String buttonText = isFree ? "🚀 Улучшить тариф" : "🔄 Продлить подписку";

        return new InlineKeyboardMarkup(List.of(
                List.of(
                        InlineKeyboardButton.builder()
                                .text(buttonText)
                                .callbackData("subscription:show_list")
                                .build()
                ),
                List.of(
                        InlineKeyboardButton.builder()
                                .text("⬅️ Назад")
                                .callbackData("navigation:back")
                                .build()
                )
        ));
    }

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

    /**
     * 1. Умная клавиатура меню подписки
     * @param currentTier Текущий тариф пользователя
     * @param subscriptionEndsAt Дата окончания подписки (может быть null для бесплатной)
     */
    public InlineKeyboardMarkup createSmartSubscriptionMenuKeyboard(
            SubscriptionTier currentTier,
            LocalDateTime subscriptionEndsAt) {

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 УСЛОВНАЯ ЛОГИКА:
        // 1. Если тариф UNLIMITED - не показываем кнопку продления
        // 2. Если тариф BASIC/PRO - показываем кнопку "Улучшить" с более дорогими тарифами
        // 3. Если тариф FREE - показываем все платные тарифы

        if (currentTier == SubscriptionTier.UNLIMITED) {
            // UNLIMITED - самый высокий тариф
            if (isSubscriptionNearExpiry(subscriptionEndsAt, 5)) {
                // Если подписка заканчивается через <=5 дней, показываем продление
                rows.add(List.of(createRenewButton()));
            } else {
                // Иначе показываем информацию о подписке
                rows.add(List.of(createSubscriptionInfoButton()));
            }
        }
        else if (currentTier == SubscriptionTier.FREE) {
            // FREE - показываем все платные тарифы
            rows.add(List.of(createUpgradeButton()));
        }
        else {
            // BASIC или PRO - показываем только более дорогие тарифы
            rows.add(List.of(createUpgradeToHigherButton(currentTier)));

            // И кнопку продления если подписка скоро закончится
            if (isSubscriptionNearExpiry(subscriptionEndsAt, 3)) {
                rows.add(List.of(createRenewButton()));
            }
        }

        // Кнопка "Назад" всегда
        rows.add(List.of(createButton("⬅️ Назад", "navigation:back")));

        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 🔥 Кнопка "Улучшить тариф" (для FREE)
     */
    private InlineKeyboardButton createUpgradeButton() {
        return createButton("🚀 Улучшить тариф", "subscription:show_list");
    }

    /**
     * 🔥 Кнопка "Улучшить до выше" (для BASIC/PRO)
     */
    private InlineKeyboardButton createUpgradeToHigherButton(SubscriptionTier currentTier) {
        String buttonText = getUpgradeButtonText(currentTier);
        String callbackData = "subscription:show_higher:" + currentTier.name();
        return createButton(buttonText, callbackData);
    }

    /**
     * 🔥 Кнопка "Продлить подписку"
     */
    private InlineKeyboardButton createRenewButton() {
        return createButton("🔄 Продлить подписку", "subscription:renew");
    }

    /**
     * 🔥 Кнопка "Информация о подписке" (для UNLIMITED)
     */
    private InlineKeyboardButton createSubscriptionInfoButton() {
        return createButton("📊 Информация о подписке", "subscription:info");
    }

    /**
     * 🔥 Получить текст кнопки в зависимости от текущего тарифа
     */
    private String getUpgradeButtonText(SubscriptionTier currentTier) {
        switch (currentTier) {
            case FREE:
                return "🚀 Улучшить тариф";
            case BASIC:
                return "⚡ Улучшить до PRO/UNLIMITED";
            case PRO:
                return "🏆 Улучшить до UNLIMITED";
            default:
                return "🚀 Улучшить тариф";
        }
    }

    /**
     * 🔥 Проверка, что подписка скоро заканчивается
     */
    private boolean isSubscriptionNearExpiry(LocalDateTime subscriptionEndsAt, int daysThreshold) {
        if (subscriptionEndsAt == null) return false;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdDate = subscriptionEndsAt.minusDays(daysThreshold);

        return !now.isBefore(thresholdDate) && !now.isAfter(subscriptionEndsAt);
    }

    /**
     * 🔥 Клавиатура со списком тарифов для улучшения (только выше текущего)
     */
    public InlineKeyboardMarkup createHigherTiersKeyboard(
            List<SubscriptionTier> allTiers,
            SubscriptionTier currentTier) {

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Фильтруем только тарифы выше текущего
        List<SubscriptionTier> higherTiers = allTiers.stream()
                .filter(tier -> tier.ordinal() > currentTier.ordinal())
                .sorted(Comparator.comparing(SubscriptionTier::ordinal))
                .toList();

        if (higherTiers.isEmpty()) {
            // Если нет более высоких тарифов, показываем сообщение
            rows.add(List.of(createButton(
                    "🏆 У вас максимальный тариф!",
                    "subscription:max_tier"
            )));
        } else {
            // Показываем доступные для улучшения тарифы
            for (SubscriptionTier tier : higherTiers) {
                String buttonText = String.format("%s (%.0f ₽/мес)",
                        tier.getDisplayName(), tier.getPrice());
                String callbackData = "subscription:select:" + tier.name();
                rows.add(List.of(createButton(buttonText, callbackData)));
            }
        }

        // Кнопка "Назад"
        rows.add(List.of(createButton("⬅️ Назад", "navigation:back")));

        return new InlineKeyboardMarkup(rows);
    }

    /**
     * 🔥 Клавиатура для продления текущего тарифа
     */
    public InlineKeyboardMarkup createRenewalKeyboard(SubscriptionTier currentTier) {
        return new InlineKeyboardMarkup(List.of(
                List.of(createButton(
                        String.format("🔄 Продлить %s (%.0f ₽)",
                                currentTier.getDisplayName(),
                                currentTier.getPrice()),
                        "subscription:renew_confirm:" + currentTier.name()
                )),
                List.of(createButton("⬅️ Назад", "navigation:back"))
        ));
    }

    private InlineKeyboardButton createButton(String text, String callback) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callback)
                .build();
    }
}
