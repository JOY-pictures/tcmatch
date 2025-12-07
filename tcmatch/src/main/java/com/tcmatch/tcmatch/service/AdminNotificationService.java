package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.events.NewVerificationRequestEvent;
import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.model.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminNotificationService {

    private final BotExecutor botExecutor;
    private final AdminService adminService;
    private final UserService userService;

    /**
     * 🔥 УВЕДОМЛЕНИЕ АДМИНАМ О НОВОЙ ЗАЯВКЕ НА ВЕРИФИКАЦИЮ
     * С КНОПКАМИ ДЛЯ ПРОВЕРКИ
     */
    @Async
    @EventListener
    public void handleNewVerificationRequest(NewVerificationRequestEvent event) {
        try {
            VerificationRequest request = event.getVerificationRequest();

            // Получаем всех админов
            List<Long> adminChatIds = adminService.getAllAdminChatIds();

            if (adminChatIds.isEmpty()) {
                log.warn("Нет админов для уведомления о заявке {}", request.getId());
                return;
            }

            // Получаем информацию о пользователе
            UserDto user = userService.getUserDtoByChatId(request.getUserChatId())
                    .orElseGet(() -> UserDto.builder()
                            .userName("неизвестно")
                            .firstName("Пользователь")
                            .build());

            // Отправляем каждому админу с кнопками действий
            for (Long adminChatId : adminChatIds) {
                try {
                    sendActionNotification(adminChatId, request, user);
                    log.debug("Уведомление отправлено админу {} о заявке {}",
                            adminChatId, request.getId());

                } catch (Exception e) {
                    log.error("Ошибка отправки уведомления админу {}: {}",
                            adminChatId, e.getMessage());
                }
            }

            log.info("✅ Уведомления о новой заявке #{} отправлены {} админам",
                    request.getId(), adminChatIds.size());

        } catch (Exception e) {
            log.error("❌ Ошибка обработки события новой заявки: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔥 ОТПРАВКА УВЕДОМЛЕНИЯ С КНОПКАМИ ДЕЙСТВИЙ
     */
    private void sendActionNotification(Long adminChatId,
                                        VerificationRequest request,
                                        UserDto user) {
        String message = formatVerificationNotification(request, user);

        // Создаем клавиатуру с действиями
        var keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.builder()
                                        .text("✅ Одобрить")
                                        .callbackData("admin:verification:approve:" + request.getId())
                                        .build(),
                                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.builder()
                                        .text("❌ Отклонить")
                                        .callbackData("admin:verification:reject:" + request.getId())
                                        .build()
                        ),
                        List.of(
                                org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.builder()
                                        .text("📋 Подробнее")
                                        .callbackData("admin:verification:details:" + request.getId())
                                        .build()
                        )
                ))
                .build();

        botExecutor.sendHtmlMessageReturnId(adminChatId, message, keyboard);
    }

    /**
     * 🔥 ФОРМАТИРОВАНИЕ СООБЩЕНИЯ ДЛЯ АДМИНА
     */
    private String formatVerificationNotification(VerificationRequest request, UserDto user) {
        return String.format("""
        🔔 <b>НОВАЯ ЗАЯВКА НА ВЕРИФИКАЦИЮ</b>
        
        <b>👤 Пользователь:</b> @%s
        <b>📛 Имя:</b> %s
        <b>🔗 GitHub:</b> <code>%s</code>
        <b>📋 Тип:</b> %s
        <b>📅 Дата:</b> %s
        <b>🔢 ID заявки:</b> <code>#%d</code>
        
        <i>Примите решение сейчас
        или перейдите в админ-панель для проверки позже → /admin</i>
        """,
                user.getUserName() != null ? user.getUserName() : "без username",
                user.getFirstName() != null ? user.getFirstName() : "Не указано",
                request.getProvidedData(),
                request.getType().getDisplayName(),
                request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                request.getId()
        );
    }

    /**
     * 🔥 УВЕДОМЛЕНИЕ АДМИНАМ О НОВОМ ОБРАЩЕНИИ В ПОДДЕРЖКУ
     * (на будущее - можно реализовать позже)
     */
    @Async
    public void notifyNewSupportTicket(Long userChatId, String userMessage) {
        try {
            List<Long> adminChatIds = adminService.getAllAdminChatIds();
            if (adminChatIds.isEmpty()) return;

            // Получаем информацию о пользователе
            UserDto user = userService.getUserDtoByChatId(userChatId).orElse(null);

            String userName = "Неизвестный пользователь";
            if (user != null) {
                userName = user.getUserName() != null ? "@" + user.getUserName() :
                        user.getFirstName() != null ? user.getFirstName() : "Пользователь";
            }

            String message = String.format("""
            🆘 <b>НОВОЕ ОБРАЩЕНИЕ В ПОДДЕРЖКУ</b>
            
            <b>👤 От:</b> %s
            <b>💬 Сообщение:</b>
            <i>%s</i>
            
            <b>📞 Chat ID:</b> <code>%d</code>
            <b>🕐 Время:</b> %s
            
            <i>Требуется ответ</i>
            """,
                    userName,
                    userMessage.length() > 500 ? userMessage.substring(0, 500) + "..." : userMessage,
                    userChatId,
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"))
            );

            for (Long adminChatId : adminChatIds) {
                botExecutor.sendHtmlMessageReturnId(adminChatId, message, null);
            }

            log.info("Уведомления о новом обращении в поддержку отправлены {} админам",
                    adminChatIds.size());

        } catch (Exception e) {
            log.error("Ошибка уведомления админов о новом обращении: {}", e.getMessage());
        }
    }
}
