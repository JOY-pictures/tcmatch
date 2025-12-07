package com.tcmatch.tcmatch.bot.commands.impl.purchase;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.dispatcher.CommandDispatcher;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.PurchaseConfirmationDto;
import com.tcmatch.tcmatch.model.enums.PurchaseType;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutePurchaseCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final WalletService walletService;
    private final CommonKeyboards commonKeyboards;

    @Lazy
    @Autowired
    private CommandDispatcher commandDispatcher; // 🔥 Инжектим диспетчер

    @Override
    public boolean canHandle(String actionType, String action) {
        return "purchase".equals(actionType) && "execute".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = context.getMessageId();
        String targetId = context.getParameter();

        try {
            // 🔥 ПОЛУЧАЕМ ДАННЫЕ ИЗ СЕССИИ
            PurchaseConfirmationDto confirmationDto = userSessionService.getPurchaseConfirmation(chatId);

            if (confirmationDto == null) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Данные покупки не найдены", 5);
                return;
            }

            // Проверяем совпадение targetId
            if (!confirmationDto.getTargetId().equals(targetId)) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Данные покупки не совпадают", 5);
                userSessionService.clearPurchaseConfirmation(chatId);
                return;
            }

            // Выполняем списание средств
            boolean success = processPurchase(chatId, confirmationDto);

            if (success) {
                // 🔥 ВЫПОЛНЯЕМ КОЛБЭК УСПЕХА ИЗ ДАННЫХ СЕССИИ
                if (confirmationDto.getSuccessCallback() != null &&
                        !confirmationDto.getSuccessCallback().isEmpty()) {

                    executeSuccessCallback(chatId, confirmationDto.getSuccessCallback(),
                            confirmationDto.getMessageId() != null ?
                                    confirmationDto.getMessageId() : messageId);
                }

                log.info("Покупка выполнена: chatId={}, type={}, amount={}, targetId={}",
                        chatId, confirmationDto.getPurchaseType(),
                        confirmationDto.getAmount(), targetId);
            }

            // Очищаем подтверждение
            userSessionService.clearPurchaseConfirmation(chatId);

        } catch (Exception e) {
            log.error("Ошибка выполнения покупки: {}", e.getMessage(), e);
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Ошибка выполнения покупки", 5);
            userSessionService.clearPurchaseConfirmation(chatId);
        }
    }

    private boolean validateConfirmation(Long chatId, PurchaseConfirmationDto confirmationDto,
                                         PurchaseType purchaseType, BigDecimal amount, String targetId) {
        if (confirmationDto == null) {
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Подтверждение не найдено", 5);
            return false;
        }

        if (!confirmationDto.getPurchaseType().equals(purchaseType) ||
                confirmationDto.getAmount().compareTo(amount) != 0 ||
                !confirmationDto.getTargetId().equals(targetId)) {

            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Данные подтверждения не совпадают", 5);
            userSessionService.clearPurchaseConfirmation(chatId);
            return false;
        }

//        if (userSessionService.isPurchaseConfirmationExpired(chatId)) {
//            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Время на подтверждение истекло", 5);
//            userSessionService.clearPurchaseConfirmation(chatId);
//            return false;
//        }

        return true;
    }

    /**
     * Обрабатывает покупку (только списание средств)
     */
    private boolean processPurchase(Long chatId, PurchaseConfirmationDto dto) {
        try {
            // Списываем средства
            walletService.withdraw(chatId, dto.getAmount());

            log.info("Средства списаны: chatId={}, type={}, amount={}, targetId={}",
                    chatId, dto.getPurchaseType(), dto.getAmount(), dto.getTargetId());
            return true;

        } catch (Exception e) {
            log.error("Ошибка списания средств: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось списать средства: " + e.getMessage(), e);
        }
    }

    /**
     * 🔥 Выполняет колбэк через CommandDispatcher
     */
    private void executeSuccessCallback(Long chatId, String successCallback, Integer messageId) {
        try {
            // successCallback уже в формате "actionType:action:parameter"
            log.info("Выполняем successCallback через dispatcher: {}", successCallback);

            // userName можно получить из базы или оставить null
            String userName = null;

            // 🔥 ВЫЗЫВАЕМ СУЩЕСТВУЮЩИЙ ДИСПЕТЧЕР
            commandDispatcher.handleCallback(chatId, successCallback, messageId, userName);

        } catch (Exception e) {
            log.error("Ошибка выполнения successCallback: {}", e.getMessage(), e);
            // Если колбэк не сработал, показываем стандартное сообщение
            String fallbackMessage = String.format("""
                ✅ <b>Оплата выполнена успешно!</b>
                
                Средства списаны с вашего баланса.
                
                <i>Приносим извинения, возникла техническая ошибка при активации.
                Обратитесь в поддержку для уточнения статуса.</i>
                """);

            botExecutor.editMessageWithHtml(chatId, botExecutor.getOrCreateMainMessageId(chatId), fallbackMessage, commonKeyboards.createToMainMenuKeyboard());
        }
    }
}