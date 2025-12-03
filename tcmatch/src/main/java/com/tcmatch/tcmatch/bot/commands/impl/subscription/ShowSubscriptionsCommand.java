package com.tcmatch.tcmatch.bot.commands.impl.subscription;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.SubscriptionKeyboards;
import com.tcmatch.tcmatch.service.SubscriptionService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowSubscriptionsCommand implements Command {

    private final BotExecutor botExecutor;
    private final UserService userService;
    private final SubscriptionService subscriptionService; // 🔥 Будет создан ниже
    private final CommonKeyboards commonKeyboards;
    private final SubscriptionKeyboards subscriptionKeyboards; // 🔥 Будет создан ниже

    @Override
    public boolean canHandle(String actionType, String action) {
        return "subscription".equals(actionType) && "show_menu".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        try {
            // 1. Получаем имя тарифа и дату истечения (из обновленного Service)
            String currentTariffDisplay = subscriptionService.getCurrentTariffName(chatId);

            // 2. Получаем характеристики тарифа (из обновленного Service)
            String featuresText = subscriptionService.getTariffFeatures(chatId);

            // 3. Формируем текст
            String text = String.format("""
                💰 <b>УПРАВЛЕНИЕ ПОДПИСКОЙ</b>
                
                <blockquote><b>Ваш текущий план:</b> <u>%s</u>
                
                %s</blockquote>
                """,
                    currentTariffDisplay,
                    featuresText
            );

            // 4. Клавиатура (кнопка "Улучшить" или "Продлить")
            // Мы передаем имя тарифа, чтобы клавиатура знала, какое действие предложить
            InlineKeyboardMarkup keyboard = subscriptionKeyboards.createSubscriptionMenuKeyboard(currentTariffDisplay);

            botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка показа меню подписок: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки подписок", 5);
        }
    }
}
