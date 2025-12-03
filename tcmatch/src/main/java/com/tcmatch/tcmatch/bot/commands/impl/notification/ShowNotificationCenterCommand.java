package com.tcmatch.tcmatch.bot.commands.impl.notification;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.service.PaginationManager;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import com.tcmatch.tcmatch.util.PaginationContextKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.tcmatch.tcmatch.util.PaginationContextKeys.NOTIFICATION_CENTER_CONTEXT_KEY;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowNotificationCenterCommand implements Command {

    private final BotExecutor botExecutor;
    private final NotificationService notificationService;
    private final UserService userService;
    private final PaginationManager paginationManager;
    private final CommonKeyboards commonKeyboards;
    private final NotificationPaginationCommand notificationPaginationCommand; // 🔥 Для вызова рендерера
    private final UserSessionService userSessionService;

    private static final String ENTITY_TYPE = "NOTIFICATION";

    @Override
    public boolean canHandle(String actionType, String action) {
        return "notification".equals(actionType) && "main".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();

        try {
//            Long userId = userService.findByChatId(chatId)
//                    .orElseThrow(() -> new RuntimeException("User not found")).getId();

            // 1. 🔥 Очищаем ID ПУШ-СООБЩЕНИЯ и удаляем его из чата
            notificationService.clearPushMessageAndSession(chatId);

            userSessionService.removeScreensOfType(chatId, "subscription");

            // 2. Получаем все ID уведомлений
            // (Вам нужно добавить в NotificationRepository метод findIdByUserIdOrderByCreatedAtDesc)
            List<Long> allNotificationIds = notificationService.getAllNotificationIds(chatId);

            if (allNotificationIds.isEmpty()) {
                botExecutor.deletePreviousMessages(chatId);
                Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
                botExecutor.editMessageWithHtml(
                        chatId,
                        mainMessageId,
                        "🔔 <b>ЦЕНТР УВЕДОМЛЕНИЙ</b>\n\n<i>У вас пока нет уведомлений.</i>",
                        commonKeyboards.createBackButton()
                );
                return;
            }

            // 3. Редирект на команду пагинации, чтобы отобразить ПЕРВУЮ страницу (0)
            paginationManager.renderIdBasedPage(
                    chatId,
                    NOTIFICATION_CENTER_CONTEXT_KEY,
                    allNotificationIds,
                    ENTITY_TYPE,
                    "init",
                    PaginationContextKeys.NOTIFICATIONS_PER_PAGE,
                    notificationPaginationCommand::renderNotificationPage // Передаем метод рендерера
            );


        } catch (Exception e) {
            log.error("❌ Ошибка при инициализации Центра уведомлений: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки Центра уведомлений.", 5);
        }
    }
}
