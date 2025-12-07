package com.tcmatch.tcmatch.bot.commands.impl.wallet;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.model.dto.WalletDto;
import com.tcmatch.tcmatch.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowUserWalletCommand implements Command {

    private final BotExecutor botExecutor;
    private final WalletService walletService; // 🔥 Инжектируем наш сервис кошелька

    @Override
    public boolean canHandle(String actionType, String action) {
        return "wallet".equals(actionType) && "show".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        // 1. Получаем данные кошелька
        WalletDto walletDto = walletService.getWalletDto(chatId);

        // 2. Формируем текст сообщения
        String walletMessage = formatWalletMessage(chatId, walletDto);

        // 3. Создаем кнопки
        InlineKeyboardMarkup markup = createWalletKeyboard();

        // 4. Отправляем или обновляем главное сообщение
        botExecutor.editMessageWithHtml(chatId, messageId, walletMessage, markup);

        log.info("✅ Пользователю {} показан кошелек. Баланс: {}", chatId, walletDto.getFormattedBalance());
    }

    private String formatWalletMessage(Long chatId, WalletDto dto) {
        return String.format("""
                💰 *Ваш кошелек*
                
                *Доступный баланс:* %s
                *Замороженные средства:* %s
                
                """,
                dto.getFormattedBalance(),
                dto.getFormattedFrozenBalance());
    }

    private InlineKeyboardMarkup createWalletKeyboard() {
        // 🔥 Кнопка для пополнения
        InlineKeyboardButton replenishButton = InlineKeyboardButton.builder()
                .text("💵 Пополнить баланс")
                // 🔥 Новая команда: wallet:replenish, которая будет генерировать URL
                .callbackData("wallet:replenish")
                .build();

        // Кнопка возврата или истории (можно добавить позже)
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build();

        List<List<InlineKeyboardButton>> keyboard = List.of(
                Collections.singletonList(replenishButton),
                Collections.singletonList(backButton)
        );

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}