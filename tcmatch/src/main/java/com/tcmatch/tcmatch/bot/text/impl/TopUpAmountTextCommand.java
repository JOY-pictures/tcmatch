package com.tcmatch.tcmatch.bot.text.impl;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.text.TextCommand;
import com.tcmatch.tcmatch.service.BalancePaymentService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.notifications.PaymentObserverService;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class TopUpAmountTextCommand implements TextCommand {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final BalancePaymentService paymentService;
    private final PaymentObserverService paymentObserverService; // Добавляем
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(Long chatId, String text) {
        if (!userSessionService.isAwaitingTopUpAmount(chatId)) {
            return false;
        }

        return isNumericInput(text);
    }

    @Override
    public void execute(Message message) {
        Long chatId = message.getChatId();
        String inputText = message.getText().trim();


        try {

            botExecutor.deleteMessage(chatId, message.getMessageId());
            // Парсим сумму
            BigDecimal amount = parseAmount(inputText);
            if (amount == null) {
                showAmountError(chatId);
                return;
            }

            // Валидируем сумму
            if (!validateAmount(chatId, amount)) {
                return;
            }



            // Обрабатываем пополнение
            processTopUp(chatId, amount);

        } catch (Exception e) {
            log.error("Ошибка обработки суммы пополнения для chatId={}: {}", chatId, e.getMessage(), e);
            handleError(chatId, e);
        }
    }

    /**
     * Обрабатывает пополнение баланса с использованием PaymentObserverService
     */
    private void processTopUp(Long chatId, BigDecimal amount) {
        new Thread(() -> {

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            try {
                // Показываем сообщение о создании платежа
                String processingMessage = String.format("""
                        ⏳ <b>Создание платежа...</b>
                        
                        Сумма: <b>%s ₽</b>
                        
                        Сообщение с платежом скоро появится
                        """, formatAmount(amount));

                botExecutor.editMessageWithHtml(chatId, mainMessageId, processingMessage, commonKeyboards.createToMainMenuKeyboard());

                // Генерируем платежную ссылку
                BalancePaymentService.PaymentInfo paymentInfo =
                        paymentService.generatePaymentUrl(chatId, amount);

                // ИСПОЛЬЗУЕМ PaymentObserverService для отправки сообщения с кнопкой
                paymentObserverService.sendPaymentLinkMessage(
                        chatId,
                        paymentInfo.getPaymentUrl(),
                        amount,
                        paymentInfo.getPaymentId()
                );

                // Очищаем состояние
                userSessionService.clearTopUpState(chatId);
                userSessionService.resetToMain(chatId);

                log.info("Пополнение инициировано через PaymentObserverService: chatId={}, amount={}",
                        chatId, amount);

            } catch (Exception e) {
                log.error("Ошибка создания платежа для chatId={}, amount={}: {}",
                        chatId, amount, e.getMessage());

                String errorMessage = String.format("""
                        ❌ <b>Ошибка создания платежа</b>
                        
                        Не удалось создать ссылку для оплаты.
                        Сумма: <b>%s ₽</b>
                        
                        Пожалуйста, попробуйте позже или обратитесь в поддержку.
                        """, formatAmount(amount));

                botExecutor.editMessageWithHtml(chatId, mainMessageId, errorMessage, commonKeyboards.createToMainMenuKeyboard());

                userSessionService.clearTopUpState(chatId);
                userSessionService.resetToMain(chatId);
                throw e;
            }
        }).start();
    }

    // Остальные методы остаются без изменений:
    private boolean isNumericInput(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String cleanText = text.replaceAll("\\s+", "").replace(",", ".");
        try {
            new BigDecimal(cleanText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BigDecimal parseAmount(String text) {
        try {
            String cleanText = text.replaceAll("\\s+", "").replace(",", ".");
            if (!cleanText.matches("^\\d+(\\.\\d{1,2})?$")) return null;
            BigDecimal amount = new BigDecimal(cleanText);
            amount = amount.setScale(2, BigDecimal.ROUND_DOWN);
            return amount.compareTo(BigDecimal.ZERO) <= 0 ? null : amount;
        } catch (Exception e) {
            return null;
        }
    }

    private void showAmountError(Long chatId) {
        String errorMessage = """
            ❌ <b>Неверный формат суммы</b>
            """;
        botExecutor.sendTemporaryErrorMessage(chatId, errorMessage, 5);
    }

    private boolean validateAmount(Long chatId, BigDecimal amount) {
        BigDecimal minAmount = new BigDecimal("100.00");
        BigDecimal maxAmount = new BigDecimal("50000.00");

        if (amount.compareTo(minAmount) < 0) {
            String error = String.format("""
                ❌ <b>Слишком маленькая сумма</b>
                
                Минимальная сумма: <b>%s ₽</b>
                Ваша сумма: <b>%s ₽</b>
                """, minAmount, amount);
            botExecutor.sendTemporaryErrorMessageWithHtml(chatId, error, 5);
            return false;
        }

        if (amount.compareTo(maxAmount) > 0) {
            String error = String.format("""
                ❌ <b>Слишком большая сумма</b>
                
                Максимальная сумма: <b>%s ₽</b>
                Ваша сумма: <b>%s ₽</b>
                """, maxAmount, amount);
            botExecutor.sendTemporaryErrorMessageWithHtml(chatId, error, 5);
            return false;
        }

        return true;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }

    private void handleError(Long chatId, Exception e) {
        botExecutor.sendTemporaryErrorMessage(chatId, "Произошла ошибка!", 5);

        userSessionService.clearTopUpState(chatId);
    }
}