package com.tcmatch.tcmatch.bot.commands.impl.subscription;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ActivateSubscriptionCommand implements Command {

    private final BotExecutor botExecutor;
    private final SubscriptionService subscriptionService;
    private final CommonKeyboards  commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "subscription".equals(actionType) && "activate".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
        SubscriptionTier newSubscription = SubscriptionTier.fromName(context.getParameter());
        try {
            subscriptionService.upgradeSubscription(chatId, newSubscription);
            String text = """
                🎉 <b>Подписка активирована!</b>
                
                Тариф: <b>%s</b>
                Срок действия: <b>30 дней</b>
                
                Спасибо за покупку! 🚀""".formatted(newSubscription.getDisplayName());

            botExecutor.editMessageWithHtml(chatId, mainMessageId, text, commonKeyboards.createToMainMenuKeyboard());
        } catch (Exception e) {
            log.error("❌ Ошибка активации подписки");
            botExecutor.sendTemporaryErrorMessage(chatId, "❌ Ошибка активации подписки", 5);
        }
    }
}
