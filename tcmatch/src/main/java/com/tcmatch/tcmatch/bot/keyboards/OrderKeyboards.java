package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.enums.PaymentType;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderKeyboards {

    // 🔥 Клавиатура для Шага 1: Выбор типа оплаты
    public InlineKeyboardMarkup createPaymentTypeChoiceKeyboard() {
        // Колбэк: order:set_type:FULL или order:set_type:MILESTONES
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("💰 " + PaymentType.FULL.getDisplayName() + "\n(один платеж в конце)")
                                .callbackData("order:set_type:" + PaymentType.FULL.name())
                                .build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("📊 " + PaymentType.MILESTONES.getDisplayName() + "\n(поэтапно)")
                                .callbackData("order:set_type:" + PaymentType.MILESTONES.name())
                                .build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("❌ Отменить принятие отклика")
                                .callbackData("main:menu")
                                .build()
                ))
                .build();
    }

    // 🔥 Клавиатура для Шага 2: Выбор количества этапов
    public InlineKeyboardMarkup createMilestoneCountChoiceKeyboard(double totalBudget) {

        // 1. РАССЧЕТ СУММЫ ПЛАТЕЖЕЙ
        // Для 2 этапов: (Бюджет / 2)
        double amountTwoMilestones = totalBudget / 2.0;
        // Для 3 этапов: (Бюджет / 3)
        double amountThreeMilestones = totalBudget / 3.0;

        // 2. ФОРМАТИРОВАНИЕ ДЛЯ КНОПОК
        String formattedTwo = formatAmount(amountTwoMilestones);
        String formattedThree = formatAmount(amountThreeMilestones);

        // Колбэк: order:set_milestones:2 или order:set_milestones:3
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("2 Этапа \n(≈ %s руб.)".formatted(formattedTwo))
                                .callbackData("order:set_milestones:2")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("3 Этапа \n(≈ %s руб.)".formatted(formattedThree))
                                .callbackData("order:set_milestones:3")
                                .build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("⬅️ Назад (к выбору типа оплаты)")
                                .callbackData("order:back_to_type") // Вернет на Шаг 1
                                .build()
                ))
                .build();
    }

    /**
     * Вспомогательный метод для красивого форматирования суммы (например, 50000 -> 50 000)
     */
    private String formatAmount(double amount) {
        // Используем DecimalFormat для форматирования числа без десятичных знаков и с разделителями тысяч
        // Локаль "ru" или "RU" может обеспечить нужный формат с пробелами
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###",
                new java.text.DecimalFormatSymbols(new java.util.Locale("ru", "RU")));
        return formatter.format(Math.round(amount));
    }

    // 🔥 Клавиатура для Шага 3: Финальное подтверждение
    public InlineKeyboardMarkup createConfirmationKeyboard() {
        // Колбэк: order:confirm_creation - здесь происходит создание Order
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("✅ Подтвердить и Создать Заказ")
                                .callbackData("order:confirm_creation")
                                .build()
                ))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("⬅️ Назад (изменить параметры)")
                                .callbackData("order:back_to_type") // Вернет на Шаг 1
                                .build()
                ))
                .build();
    }

    // 🔥 1. КЛАВИАТУРА ДЛЯ ЗАКАЗЧИКА (Статус: ACTIVE)
    /**
     * Предоставляет Заказчику кнопки "Оплатить" и "Заказ выполнен".
     *
     * @param orderId ID заказа.
     * @param paymentUrl Ссылка для оплаты внешним сервисом.
     * @return InlineKeyboardMarkup
     */
    public InlineKeyboardMarkup createCustomerActiveOrderKeyboard(Long orderId, String paymentUrl) {
        // Кнопка 1: Оплатить (Внешняя ссылка)
        InlineKeyboardButton payButton = InlineKeyboardButton.builder()
                .text("💳 Оплатить следующий этап")
//                .url(paymentUrl) // Внешняя ссылка для оплаты
                .callbackData("order:pay_temp:" + orderId)
                .build();

//        // Кнопка 2: Заказ выполнен (Колбэк для завершения заказа)
//        InlineKeyboardButton completeButton = InlineKeyboardButton.builder()
//                .text("✅ Заказ выполнен")
//                // order:complete:ID - команда для обработки завершения заказа
//                .callbackData("order:complete:" + orderId)
//                .build();

        // Кнопка 3: Назад в меню (если Заказчик захочет покинуть экран)
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text("◀️ Назад")
                .callbackData("navigation:back")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(payButton))
//                .keyboardRow(List.of(completeButton))
                .keyboardRow(List.of(backButton))
                .build();
    }

    // ---

    // 🔥 2. КЛАВИАТУРА ДЛЯ ИСПОЛНИТЕЛЯ (Статус: ACTIVE)
    /**
     * Предоставляет Исполнителю кнопку "Готов к сдаче".
     *
     * @param orderId ID заказа.
     * @return InlineKeyboardMarkup
     */
    public InlineKeyboardMarkup createFreelancerActiveOrderKeyboard(Long orderId) {
        // Кнопка 1: Готов к сдаче (Колбэк для уведомления Заказчика)
//        InlineKeyboardButton readyButton = InlineKeyboardButton.builder()
//                .text("🚀 Готов к сдаче этапа")
//                // order:ready:ID - команда для обработки готовности к сдаче
//                .callbackData("order:ready:" + orderId)
//                .build();

        // Кнопка 2: Назад в меню
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text("◀️ Назад")
                .callbackData("navigation:back")
                .build();

        return InlineKeyboardMarkup.builder()
//                .keyboardRow(List.of(readyButton))
                .keyboardRow(List.of(backButton))
                .build();
    }
}
