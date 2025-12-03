package com.tcmatch.tcmatch.bot.commands.impl.subscription;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.SubscriptionKeyboards;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowSubscriptionListCommand implements Command {

    private final BotExecutor botExecutor;
    private final SubscriptionService subscriptionService;
    private final SubscriptionKeyboards subscriptionKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "subscription".equals(actionType) && "show_list".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

        // 1. Получаем список платных тарифов из Service
        List<SubscriptionTier> plans = subscriptionService.getAvailablePaidPlans();

        // 2. Рендерим все тарифы
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("🚀 <b>ДОСТУПНЫЕ ТАРИФЫ</b>\n\n");
        textBuilder.append("Выберите план, чтобы улучшить свои возможности:\n\n");

        for (SubscriptionTier tier : plans) {
            textBuilder.append(formatPlanDetails(tier)).append("\n---\n");
        }

        // 3. Клавиатура с кнопками выбора (используем имя Enum для callback)
        InlineKeyboardMarkup keyboard = subscriptionKeyboards.createSubscriptionListKeyboard(plans);

        botExecutor.editMessageWithHtml(chatId, messageId, textBuilder.toString(), keyboard);
    }

    // Вспомогательный метод для форматирования деталей тарифа
    private String formatPlanDetails(SubscriptionTier tier) {
        String limit = tier.getMonthlyApplicationLimit() == Integer.MAX_VALUE ? "Безлимитно" : String.valueOf(tier.getMonthlyApplicationLimit());
        String priority = tier.isHasPriorityVisibility() ? "Да" : "Нет";

        return """
                <blockquote><b>%s</b> | %.0f ₽/мес
                
                — Особенности:
                • Отклики в месяц: <code>%s</code>
                
                • Мгновенные уведомления: <code>%s</code>
                
                • Приоритет в поиске: <code>%s</code></blockquote>
                """.formatted(
                tier.getDisplayName(),
                tier.getPrice(),
                limit,
                tier.isHasInstantNotifications() ? "Да" : "Нет",
                priority
        );
    }
}