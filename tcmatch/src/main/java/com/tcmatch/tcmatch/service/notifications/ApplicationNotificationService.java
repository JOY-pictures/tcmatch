package com.tcmatch.tcmatch.service.notifications;

import com.tcmatch.tcmatch.events.ApplicationStatusChangedEvent;
import com.tcmatch.tcmatch.events.NewApplicationEvent;
import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.NotificationService;
import com.tcmatch.tcmatch.service.OrderService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationNotificationService {

    // 🔥 Инжектируем зависимости
    private final NotificationService notificationService; // Для сохранения и "Умного пуша"
    private final OrderService orderService;               // Для получения данных Order
    private final UserService userService;                  // Для получения данных User

    /**
     * 🔥 ОБРАБОТКА ИЗМЕНЕНИЯ СТАТУСА ОТКЛИКА (ПРИНЯТ/ОТКЛОНЕН)
     */
    @Async
    @EventListener
    public void handleApplicationStatusChange(ApplicationStatusChangedEvent event) {

        Long freelancerChatId = event.getApplicationDto().getFreelancer().getChatId();

        try {
            String text;
            String callbackData = "application:details:" + event.getApplicationDto().getId();

            if (event.getNewStatus() == UserRole.ApplicationStatus.ACCEPTED) {

                // 1. ПОЛУЧАЕМ ЗАКАЗ
                Order order = orderService.findByApplicationId(event.getApplicationDto().getId())
                        .orElseThrow(() -> new RuntimeException("Заказ не найден!"));

                // 2. ПОЛУЧАЕМ КОНТАКТЫ
                UserDto customer = userService.getUserDtoByChatId(order.getCustomerChatId())
                        .orElseThrow(() -> new RuntimeException("Customer not found."));
                UserDto freelancer = userService.getUserDtoByChatId(freelancerChatId)
                        .orElseThrow(() -> new RuntimeException("Freelancer not found."));

                // 3. Генерация богатого HTML-сообщения (с контактами и правилами)
                text = createFreelancerOrderNotification(order, customer, freelancer);

                // 4. Callback теперь ведет на детали заказа
                callbackData = "order:details:" + order.getId();

            } else if (event.getNewStatus() == UserRole.ApplicationStatus.REJECTED) {
                text = String.format("Ваш отклик на проект «%s» был ОТКЛОНЕН.", event.getApplicationDto().getProject().getTitle());
            } else {
                return;
            }

            // 5. Сохраняем и вызываем "Умный пуш" через центральный NotificationService
            notificationService.createNotification(freelancerChatId, text, callbackData);

        } catch (Exception e) {
            log.error("❌ Ошибка обработки события ACCEPTED/REJECTED отклика для {}: {}", freelancerChatId, e.getMessage(), e);
        }
    }

    /**
     * 🔥 ОБРАБОТКА НОВОГО ОТКЛИКА ДЛЯ ЗАКАЗЧИКА
     */
    @Async
    @EventListener
    public void handleNewApplication(NewApplicationEvent event) {
        try {
            ApplicationDto application = event.getApplicationDto();

            String text = String.format(
                    "📨 <b>Новый отклик</b> на проект <i>«%s»</i>\n\n" +
                            "👤 Исполнитель: %s\n" +
                            "💰 Предложил: %.0f руб\n" +
                            "⏱️ Срок: %d дней",
                    application.getProject().getTitle(),
                    application.getFreelancer().getDisplayName() != null ?
                            application.getFreelancer().getDisplayName() : "Аноним",
                    application.getProposedBudget(),
                    application.getProposedDays()
            );

            String callbackData = "application:details:" + application.getId();

            // 🔥 Отправляем уведомление заказчику
            notificationService.createNotification(
                    application.getProject().getCustomerChatId(),
                    text,
                    callbackData
            );

            log.info("✅ Уведомление отправлено заказчику {} о новом отклике",
                    application.getProject().getCustomerChatId());

        } catch (Exception e) {
            log.error("❌ Ошибка уведомления заказчика о новом отклике: {}", e.getMessage(), e);
        }
    }


    /**
     * 🔥 ВСПОМОГАТЕЛЬНЫЙ МЕТОД: Форматирование уведомления о созданном заказе для Исполнителя
     */
    private String createFreelancerOrderNotification(Order order, UserDto customer, UserDto freelancer) {
        // Логика из предыдущего шага, перенесенная сюда
        return String.format("""
            <blockquote><b>🥳 ПОЗДРАВЛЯЕМ! ВАШ ОТКЛИК ПРИНЯТ!</b>
            
            Заказчик принял ваш отклик на проект <code>%d</code>.
            Создан новый заказ №%d.
            
            <b>⚠️ Ссылка на оплату первого этапа/полной суммы (на Ваш кошелек ЮMoney) УЖЕ отправлена Заказчику.</b>
            
            <b>ЭТО КРИТИЧЕСКИ ВАЖНО:</b> Заказчик должен оплатить ТОЛЬКО по этой ссылке, чтобы транзакция была засчитана в Вашу <b>РЕПУТАЦИЮ</b>.
            
            <b>--- КОНТАКТЫ ЗАКАЗЧИКА ---</b>
            👤 Имя: <b>%s</b>
            📞 Telegram (ссылка): <a href=\"tg://user?id=%d\">@%s</a>
            
            <b>Ваша договоренность:</b>
            💰 Бюджет: <code>%.0f руб</code> | Схема: %s
            
            <b>⚠️ НАЧНИТЕ РАБОТУ!</b>
            
            Свяжитесь с заказчиком.</blockquote>
            """,
                order.getProjectId(),
                order.getId(),
                customer.getFirstName(),
                customer.getChatId(),
                customer.getUserName() != null ? customer.getUserName() : "Имя скрыто",
                order.getTotalBudget(),
                order.getPaymentType().getDisplayName().toLowerCase()
        );
    }
}