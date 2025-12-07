package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.SubscriptionKeyboards;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import com.tcmatch.tcmatch.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfirmApplicationCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationCreationService applicationCreationService;
    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final SubscriptionService subscriptionService;
    private final CommonKeyboards commonKeyboards;
    private final SubscriptionKeyboards subscriptionKeyboards;
    private final ApplicationKeyboards applicationKeyboards;
    private final UserSessionService userSessionService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "confirm".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
            ApplicationCreationState state = applicationCreationService.getCurrentState(chatId);
            if (state == null) return;

            if (!state.isCompleted()) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Заполните все поля отклика", 5);
                return;
            }

            // 🔥 ==========================================================
            // 🔥 ШАГ 1: ПРОВЕРКА ЛИМИТОВ
            // 🔥 ==========================================================
            if (!subscriptionService.hasSufficientApplications(chatId)) {

                SubscriptionService.SubscriptionStatsDto currentStats = subscriptionService.getSubscriptionStats(chatId);
                String warningText = createSubscriptionWarningText(currentStats);

                botExecutor.editMessageWithHtml(chatId, messageId, warningText,
                        subscriptionKeyboards.createSubscriptionKeyboard());
                return;
            }

            // 🔥 ШАГ 2: ИСПОЛЬЗУЕМ ОТКЛИК (уменьшаем лимит)
            // Мы заменили логику checkApplicationLimits и useApplication на наши методы
            boolean applicationUsed = subscriptionService.useApplication(chatId);

            if (!applicationUsed) {
                // Если useApplication вернул false, значит decrementApplicationCount провалился (IllegalStateException)
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ Не удалось использовать отклик. Повторите попытку.", 5);
                return;
            }

            // СОЗДАЕМ ОТКЛИК
            Application application = applicationService.createApplication(
                    state.getProjectId(),
                    chatId,
                    state.getCoverLetter(),
                    state.getProposedBudget(),
                    state.getProposedDays()
            );

            applicationCreationService.completeCreation(chatId);

            // 🔥 ШАГ 4: ОБНОВЛЯЕМ СТАТИСТИКУ ДЛЯ СООБЩЕНИЯ УСПЕХА
            SubscriptionService.SubscriptionStatsDto updatedStats = subscriptionService.getSubscriptionStats(chatId);

            // 🔥 ПОЛУЧАЕМ ДАННЫЕ ПРОЕКТА ЧЕРЕЗ СЕРВИС
            String projectTitle = projectService.getProjectTitleById(state.getProjectId());

            // 🔥 ФОРМИРУЕМ СООБЩЕНИЕ УСПЕХА
            String limitDisplay = updatedStats.getMonthlyLimit() == -1
                    ? "Безлимитно"
                    : String.format("<code>%d/%d</code>", updatedStats.getRemainingApplications(), updatedStats.getMonthlyLimit());

            String successText = """
                    <b>✅ ОТКЛИК ОТПРАВЛЕН!</b>

                    <blockquote><b>💼 Проект:</b> %s
                    <b>💰 Ваш бюджет:</b> <code>%.0f руб</code>
                    <b>⏱️ Ваш срок:</b> <code>%d дней</code>
                
                    <b>📨 Статус:</b> отправлен заказчику
                    <b>⏳ Ожидание:</b> ответа от заказчика </blockquote>
                
                    <b>📊 Осталось откликов в этом месяце:</b> %s
                    <b>💡 Тариф:</b> <i>%s</i>
                
                    <i>💡 Лимит обновится %s</i>
                    """.formatted(
                    escapeHtml(projectTitle),
                    application.getProposedBudget(),
                    application.getProposedDays(),
                    limitDisplay,
                    updatedStats.getTier().getDisplayName(), // Добавляем название тарифа
                    updatedStats.formatResetDate()
            );

            userSessionService.clearNavigationHistory(chatId);

            botExecutor.editMessageWithHtml(chatId, messageId, successText, commonKeyboards.createToMainMenuKeyboard());

            log.info("✅ Пользователь {} откликнулся на проект {}", chatId, state.getProjectId());

        } catch (Exception e) {
            log.error("❌ Ошибка подтверждения отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка отправки отклика: " + e.getMessage(), 5);
        }
    }

    // 🔥 ФОРМАТИРОВАНИЕ ДАТЫ ОБНОВЛЕНИЯ ЛИМИТОВ
    private String formatNextResetDate() {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
        return nextMonth.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // 🔥 ТЕКСТ ПРЕДУПРЕЖДЕНИЯ О ЛИМИТАХ
        private String createSubscriptionWarningText(SubscriptionService.SubscriptionStatsDto check) {

            // Получаем тарифы для вывода
            SubscriptionTier basic = SubscriptionTier.BASIC;
            SubscriptionTier pro = SubscriptionTier.PRO;

            String usedCount = check.getMonthlyLimit() == -1
                    ? "не ограничено"
                    : String.valueOf(check.getMonthlyLimit() - check.getRemainingApplications());

            return """
            ⚠️<b> **ЛИМИТ ОТКЛИКОВ ИСЧЕРПАН**</b>
            
            📊 <b>Ваш текущий тариф: *%s*</b>
            🚫 Использовано откликов: *%s/%s*
            
            <b>💎 *Что делать:*</b>
            • Приобрести подписку <b>TCMatch Pro</b>
            • <i>Дождаться обновления лимита (%s)</i>
            
            🛒 <b>*Доступные тарифы:*</b>
            • <b>%s</b>: %d откликов | <code>%.0f руб</code>
            • <b>%s</b>: %d откликов + приоритет | <code>%.0f руб</code>
            • <b>%s</b>: Безлимитно + приоритет | <code>%.0f руб</code>
            
            <b>💡 *Подписка открывает:*
            • Больше откликов в месяц
            • Приоритет в поиске (PRO/UNL)
            • Мгновенные уведомления (PRO/UNL)</b>
            """.formatted(
                    check.getTier().getDisplayName(),
                    usedCount,
                    check.getMonthlyLimit() == -1 ? "∞" : String.valueOf(check.getMonthlyLimit()),
                    check.formatResetDate(),

                    basic.getDisplayName(), basic.getMonthlyApplicationLimit(), basic.getPrice(),
                    pro.getDisplayName(), pro.getMonthlyApplicationLimit(), pro.getPrice(),
                    pro.getDisplayName(), 0, pro.getPrice() // 0 для UNL выглядит лучше, чем Integer.MAX_VALUE
            );
    }
}
