package com.tcmatch.tcmatch.bot.commands.impl.subscription;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.SubscriptionKeyboards;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.SubscriptionPaymentService;
import com.tcmatch.tcmatch.service.SubscriptionService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.notifications.PaymentObserverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SelectSubscriptionCommand implements Command {

    private final BotExecutor botExecutor;
    private final SubscriptionService subscriptionService;
    private final SubscriptionPaymentService paymentService;
    private final SubscriptionKeyboards subscriptionKeyboards;
    private final UserSessionService userSessionService;
    private final CommonKeyboards commonKeyboards;
    private final PaymentObserverService paymentObserverService; // 🔥 Добавили

    @Override
    public boolean canHandle(String actionType, String action) {
        return "subscription".equals(actionType) && "select".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);

        // 1. Получаем имя выбранного ENUM из параметра callback'а (например, "PRO")
        String selectedTierName = context.getParameter();

        SubscriptionTier selectedTier = subscriptionService.getTierByName(selectedTierName)
                .orElseThrow(() -> new RuntimeException("Выбранный тариф не найден: " + selectedTierName));

        // 2. Получаем текущий активный тариф пользователя
        SubscriptionTier currentTier = subscriptionService.getVerifiedSubscriptionTier(chatId);

        // 3. 🔥 ЛОГИКА АПГРЕЙДА И РАСЧЕТ СУММЫ
        Double amountToPay = selectedTier.getPrice();
        String paymentType = "покупку";

        if (selectedTier.ordinal() > currentTier.ordinal() && currentTier != SubscriptionTier.FREE) {
            paymentType = "улучшение";
        } else if (selectedTier.equals(currentTier) && selectedTier != SubscriptionTier.FREE) {
            paymentType = "продление";
        }

        // 🔥 4. ПОКАЗЫВАЕМ "ОЖИДАНИЕ ОПЛАТЫ" В ГЛАВНОМ СООБЩЕНИИ
        String processingText = String.format("""
            ⏳ <b>ФОРМИРОВАНИЕ ССЫЛКИ ДЛЯ ОПЛАТЫ</b>
            
            Тариф: <b>%s</b>
            Сумма: <b>%.0f ₽</b>
            Тип операции: <b>%s</b>
            
            <i>Скоро придёт сообщение с платежом...</i>
            """,
                selectedTier.getDisplayName(),
                amountToPay,
                paymentType
        );

        // 🔥 5. КЛАВИАТУРА С КНОПКОЙ "ДОМОЙ"
        InlineKeyboardMarkup homeKeyboard = commonKeyboards.createToMainMenuKeyboard();

        userSessionService.resetToMain(chatId);

        // Редактируем главное сообщение
        botExecutor.editMessageWithHtml(chatId, mainMessageId, processingText, homeKeyboard);
        log.info("📝 Показано 'Ожидание оплаты' для chatId={}, тариф={}", chatId, selectedTier);

        // 🔥 6. ГЕНЕРАЦИЯ ССЫЛКИ И ОТПРАВКА СООБЩЕНИЯ С ОПЛАТОЙ (АСИНХРОННО)
        sendPaymentLinkAsync(chatId, selectedTier, amountToPay);
    }

    private void sendPaymentLinkAsync(Long chatId, SubscriptionTier tier, Double amount) {
        new Thread(() -> {
            try {
                // 🔥 1. Используем новый метод, который возвращает paymentId
                SubscriptionPaymentService.PaymentInfo paymentInfo =
                        paymentService.generatePaymentUrl(chatId, tier, amount);

                String paymentUrl = paymentInfo.getPaymentUrl();
                String paymentId = paymentInfo.getPaymentId();

                log.info("💰 Создан платеж: paymentId={}, chatId={}, tier={}",
                        paymentId, chatId, tier);

                // 🔥 2. Отправка сообщения с кнопкой оплаты через PaymentObserverService
                paymentObserverService.sendPaymentLinkMessage(chatId, paymentUrl, tier, paymentId);

                log.info("💳 Платежное сообщение отправлено: chatId={}, tier={}", chatId, tier);

            } catch (Exception e) {
                log.error("❌ Ошибка при создании платежа: {}", e.getMessage(), e);

                // Показываем ошибку в главном сообщении
                String errorText = String.format("""
                ❌ <b>ОШИБКА ПРИ СОЗДАНИИ ПЛАТЕЖА</b>
                
                Тариф: <b>%s</b>
                
                Не удалось создать ссылку для оплаты.
                Пожалуйста, попробуйте позже или свяжитесь с поддержкой.
                """,
                        tier.getDisplayName()
                );

                Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
                botExecutor.editMessageWithHtml(chatId, mainMessageId, errorText,
                        commonKeyboards.createToMainMenuKeyboard());
            }
        }).start();
    }

    /**
     * 🔥 Временно: генерируем paymentId или получаем из БД
     * Нужно обновить SubscriptionPaymentService чтобы он возвращал paymentId
     */
    private String extractPaymentIdFromTransaction(Long chatId, SubscriptionTier tier) {
        // Временно используем UUID
        // В реальности нужно получать из транзакции в БД
        return "payment_" + UUID.randomUUID().toString().substring(0, 8);
    }
}