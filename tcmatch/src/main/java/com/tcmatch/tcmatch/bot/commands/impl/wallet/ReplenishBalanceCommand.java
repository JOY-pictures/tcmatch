package com.tcmatch.tcmatch.bot.commands.impl.wallet;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.BalancePaymentService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplenishBalanceCommand implements Command {

    private final BotExecutor botExecutor;
    private final BalancePaymentService paymentService;
    private final CommonKeyboards commonKeyboards;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        // 🔥 Обрабатываем нажатие на кнопку "Пополнить баланс"
        return "wallet".equals(actionType) && "replenish".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();

        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        askTopUpAmount(chatId, messageId);
    }
    /**
     * Запрос суммы для пополнения
     */
    private void askTopUpAmount(Long chatId, Integer messageId) {
        try {
            // Отправляем запрос суммы
            String message = """
                💰<b> *Введите сумму для пополнения*</b>
                
                <i>*Минимальная сумма:* 100
                *Максимальная сумма:* 50 000</i>
                """;

            // Отправляем сообщение с запросом
            botExecutor.editMessageWithHtml(chatId, messageId, message, commonKeyboards.createToMainMenuKeyboard());

            // Устанавливаем состояние ожидания ввода суммы
            // messageId - это ID сообщения, которое нужно будет удалить при вводе суммы
            userSessionService.setAwaitingTopUpAmount(chatId, messageId);

        } catch (Exception e) {
            log.error("Ошибка запроса суммы пополнения: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Ошибка: " + e.getMessage(), 5);
        }
    }
}