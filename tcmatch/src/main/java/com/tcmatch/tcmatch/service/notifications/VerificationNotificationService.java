package com.tcmatch.tcmatch.service.notifications;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.events.VerificationStatusChangedEvent;
import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationNotificationService {

    private final BotExecutor botExecutor;
    private final NotificationService notificationService;

    /**
     * 🔥 УВЕДОМЛЕНИЕ ПОЛЬЗОВАТЕЛЮ О РЕЗУЛЬТАТЕ ПРОВЕРКИ
     * Сохраняет уведомление в БД через общий NotificationService
     */
    @Async
    @EventListener
    public void handleVerificationStatusChanged(VerificationStatusChangedEvent event) {
        try {
            VerificationRequest request = event.getVerificationRequest();
            Long adminChatId = event.getAdminChatId();

            // Формируем сообщение
            String messageText = formatUserNotification(request, adminChatId);

            // Сохраняем уведомление через общий сервис (в БД и умный пуш)
            notificationService.createNotification(request.getUserChatId(), messageText, "");

            log.info("✅ Уведомление о результате проверки заявки #{} сохранено для пользователя {}",
                    request.getId(), request.getUserChatId());

        } catch (Exception e) {
            log.error("❌ Ошибка создания уведомления о результате проверки: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * 🔥 ФОРМАТИРОВАНИЕ УВЕДОМЛЕНИЯ ДЛЯ ПОЛЬЗОВАТЕЛЯ
     */
    private String formatUserNotification(VerificationRequest request, Long adminChatId) {
        String statusText;
        String additionalInfo = "";

        if (request.getStatus().name().equals("APPROVED")) {
            statusText = "✅ <b>ВЕРИФИКАЦИЯ ОДОБРЕНА</b>";
            additionalInfo = """
            
            <b>🎉 Поздравляем!</b>
            Теперь ваш профиль отмечен как верифицированный.
            Это повышает доверие заказчиков и увеличивает ваши шансы на получение проектов.
            """;
        } else if (request.getStatus().name().equals("REJECTED")) {
            statusText = "❌ <b>ВЕРИФИКАЦИЯ ОТКЛОНЕНА</b>";

            if (request.getAdminComment() != null && !request.getAdminComment().isEmpty()) {
                additionalInfo = String.format("""
                
                <b>Причина:</b>
                <i>%s</i>
                
                <b>Вы можете:</b>
                1. Исправить ошибки и подать заявку снова
                2. Обратиться в поддержку для уточнений
                """, request.getAdminComment());
            }
        } else {
            statusText = "⏳ <b>СТАТУС ИЗМЕНЕН</b>";
        }

        return String.format("""
        %s
        
        <b>🔗 GitHub:</b> <code>%s</code>
        <b>📅 Дата проверки:</b> %s
        <b>🔢 ID заявки:</b> <code>#%d</code>
        %s
        
        <i>Спасибо за использование нашей платформы!</i>
        """,
                statusText,
                request.getProvidedData(),
                request.getReviewedAt() != null ?
                        request.getReviewedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                        "не указано",
                request.getId(),
                additionalInfo
        );
    }
}