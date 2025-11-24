package com.tcmatch.tcmatch.bot.commands.impl.notification;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.NotificationKeyboards;
import com.tcmatch.tcmatch.model.Notification;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.service.PaginationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.NOTIFICATIONS_PER_PAGE;
import static com.tcmatch.tcmatch.util.PaginationContextKeys.NOTIFICATION_CENTER_CONTEXT_KEY;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationPaginationCommand implements Command {

    private final BotExecutor botExecutor;
    private final PaginationManager paginationManager;
    private final CommonKeyboards commonKeyboards;
    private final NotificationKeyboards notificationKeyboards;
    private final NotificationService notificationService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "notification".equals(actionType) && "pagination".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            // Формат: "next:notification_center:NOTIFICATION"
            String[] parts = context.getParameter().split(":");
            if (parts.length < 3) return;

            String direction = parts[0];
            String contextKey = parts[1];
            String entityType = parts[2];

            // 🔥 ОПРЕДЕЛЯЕМ РЕНДЕРЕР (логика пагинации откликов, которую ты прислал)
            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = null;

            if (NOTIFICATION_CENTER_CONTEXT_KEY.equals(contextKey)) {
                renderer = this::renderNotificationPage;
            }

            if (renderer == null) {
                log.error("❌ Renderer not found for notification context: {}", contextKey);
                return;
            }

            // 🔥 ВЫЗЫВАЕМ PAGINATION MANAGER
            paginationManager.renderIdBasedPage(
                    context.getChatId(),
                    contextKey,
                    null, // ID уже в контексте
                    entityType,
                    direction,
                    NOTIFICATIONS_PER_PAGE,
                    renderer
            );

        } catch (Exception e) {
            log.error("❌ Ошибка пагинации уведомлений: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка переключения страницы", 5);
        }
    }

    // 🔥 МЕТОД РЕНДЕРИНГА
    public List<Integer> renderNotificationPage(List<Long> pageNotificationIds, PaginationContext context) {
        Long chatId = context.chatId();
        List<Integer> messageIds = new ArrayList<>();

        // 1. Очистка предыдущих сообщений (по твоей логике из ApplicationPaginationCommand)
        botExecutor.deletePreviousMessages(chatId);

        // 2. Получаем DTO/Entity по ID
        List<Notification> notifications = notificationService.getNotificationsByIds(pageNotificationIds);

        // 3. Заголовок
        String headerText = String.format("""
            🔔 <b>ЦЕНТР УВЕДОМЛЕНИЙ</b>
            
            <i>Найдено %d уведомлений. Страница %d из %d</i>
            """, context.entityIds().size(), context.currentPage() + 1, context.getTotalPages());

        Integer headerId = botExecutor.getOrCreateMainMessageId(chatId);
        botExecutor.editMessageWithHtml(chatId, headerId, headerText, null);

        // 4. Карточки уведомлений
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm");

        for (Notification n : notifications) {
            String statusIcon = n.getStatus().name().equals("READ") ? "⚫" : "🔴";

            String notificationText = String.format("""
                %s <b>#%d </b> (<i>%s</i>)
                
                <b>%s</b>
                """,
                    statusIcon,
                    n.getId(),
                    n.getCreatedAt().format(formatter),
                    n.getText()
            );

            // Клавиатура действий (Посмотреть/Удалить)
            InlineKeyboardMarkup keyboard = notificationKeyboards.createNotificationItemKeyboard(n.getId());

            Integer cardId = botExecutor.sendHtmlMessageReturnId(chatId, notificationText, keyboard);
            if (cardId != null) messageIds.add(cardId);
        }

        // 5. Пагинация
        InlineKeyboardMarkup paginationKeyboard = commonKeyboards.createPaginationKeyboardForContext(context);

        Integer navId = botExecutor.sendHtmlMessageReturnId(chatId, "<b>— Навигация —</b>", paginationKeyboard);
        if (navId != null) messageIds.add(navId);

        return messageIds;
    }
}