package com.tcmatch.tcmatch.bot.commands.impl.purchase;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.PurchaseConfirmationDto;
import com.tcmatch.tcmatch.model.dto.WalletDto;
import com.tcmatch.tcmatch.model.enums.PurchaseType;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestPurchaseConfirmationCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final WalletService walletService;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "purchase".equals(actionType) && "confirm".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = context.getMessageId();
        String parameter = context.getParameter();

        try {

            // 🔥 ПОЛУЧАЕМ ДАННЫЕ ИЗ СЕССИИ
            PurchaseConfirmationDto confirmationDto = userSessionService.getPurchaseConfirmation(chatId);
            if (confirmationDto == null) {
                log.error("Данные подтверждения не найдены в сессии: chatId={}", chatId);
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Данные покупки не найдены. Начните заново.", 5);
                return;
            }

            // Проверяем, что targetId совпадает
            if (!confirmationDto.getTargetId().equals(parameter)) {
                log.error("TargetId не совпадает: session={}, callback={}",
                        confirmationDto.getTargetId(), parameter);
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Неверные данные покупки", 5);
                userSessionService.clearPurchaseConfirmation(chatId);
                return;
            }

            // Получаем данные кошелька
            WalletDto walletDto = walletService.getWalletDto(chatId);

            // Показываем окно подтверждения
            showConfirmationDialog(chatId, confirmationDto, walletDto);

            log.info("Показ подтверждения покупки: chatId={}, type={}, amount={}",
                    chatId, confirmationDto.getPurchaseType(), confirmationDto.getAmount());

        } catch (Exception e) {
            log.error("Ошибка показа подтверждения покупки: {}", e.getMessage(), e);
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Ошибка обработки запроса", 5);
            userSessionService.clearPurchaseConfirmation(chatId);
        }
    }

    private PurchaseType validatePurchaseType(String typeStr) {
        try {
            return PurchaseType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Неизвестный тип покупки: {}", typeStr);
            return null;
        }
    }

    private BigDecimal validateAmount(Long chatId, String amountStr) {
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Сумма должна быть больше нуля", 5);
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            log.error("Неверный формат суммы: {}", amountStr);
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Неверный формат суммы", 5);
            return null;
        }
    }

    /**
     * Показывает диалог подтверждения покупки
     */
    private void showConfirmationDialog(Long chatId, PurchaseConfirmationDto dto, WalletDto walletDto) {
        BigDecimal availableBalance = walletDto.getBalance();
        BigDecimal amount = dto.getAmount();

        // Проверяем достаточно ли средств
        if (availableBalance.compareTo(amount) < 0) {
            String insufficientMessage = String.format("""
                ❌ <b>Недостаточно средств</b>
                
                Требуется: <b>%s ₽</b>
                Доступно: <b>%s ₽</b>
                
                Необходимо пополнить баланс на <b>%s ₽</b>
                """,
                    formatAmount(amount),
                    formatAmount(availableBalance),
                    formatAmount(amount.subtract(availableBalance))
            );

            botExecutor.editMessageWithHtml(chatId, botExecutor.getOrCreateMainMessageId(chatId), insufficientMessage, commonKeyboards.createToMainMenuKeyboard());
            return;
        }

        String message = String.format("""
            🔔 <b>Подтверждение покупки</b>
            
            Тип: <b>%s</b>
            Сумма: <b>%s ₽</b>
            
            <i>%s</i>
            
            💰 <b>Ваш баланс</b>
            Доступно: <b>%s ₽</b>
            После списания: <b>%s ₽</b>
            
            Подтверждаете списание средств?
            """,
                dto.getPurchaseType().getDisplayName(),
                formatAmount(amount),
                dto.getDescription(),
                formatAmount(availableBalance),
                formatAmount(availableBalance.subtract(amount))
        );

        // Создаем клавиатуру
        InlineKeyboardMarkup keyboard = createConfirmationKeyboard(dto);

        botExecutor.editMessageWithHtml(chatId, botExecutor.getOrCreateMainMessageId(chatId), message, keyboard);

        // Сохраняем данные в сессию
        userSessionService.setPurchaseConfirmation(chatId, dto);
    }

    /**
     * Создает клавиатуру для подтверждения
     */
    private InlineKeyboardMarkup createConfirmationKeyboard(PurchaseConfirmationDto dto) {
        // 🔥 ТЕПЕРЬ КОЛБЭКИ ТОЖЕ КОРОТКИЕ!
        // execute: просто указываем targetId
        String executeCallback = String.format("purchase:execute:%s", dto.getTargetId());

        // cancel: просто указываем targetId
        String cancelCallback = String.format("purchase:cancel:%s", dto.getTargetId());

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.builder()
                                        .text("✅ Подтвердить списание")
                                        .callbackData(executeCallback)
                                        .build()
                        ),
                        List.of(
                                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.builder()
                                        .text("❌ Отмена")
                                        .callbackData(cancelCallback)
                                        .build()
                        )
                ))
                .build();
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }
}