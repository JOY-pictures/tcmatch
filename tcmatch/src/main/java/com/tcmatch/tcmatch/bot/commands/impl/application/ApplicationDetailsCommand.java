package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.commands.impl.order.OrderDetailsCommand;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.OrderService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationDetailsCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final ApplicationKeyboards applicationKeyboards;
    private final OrderService orderService;
    private final OrderDetailsCommand orderDetailsCommand;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "details".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
            Long applicationId = Long.parseLong(context.getParameter());
            ApplicationDto application = applicationService.getApplicationDtoById(applicationId);

            // --- Новые переменные для определения роли ---
            Long customerChatId = application.getProject().getCustomerChatId();
            Long freelancerChatId = application.getFreelancer().getChatId();

            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ОТКЛИКА
            if (!chatId.equals(freelancerChatId) && !chatId.equals(customerChatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ У вас нет доступа к этому отклику", 5);
                return;
            }

            // 🔥 УДАЛЯЕМ ПРЕДЫДУЩИЕ СООБЩЕНИЯ
            botExecutor.deletePreviousMessages(chatId);

            // 2. 🔥 ГЕНИАЛЬНАЯ ЛОГИКА: Проверяем статус отклика
            if (application.getStatus() == UserRole.ApplicationStatus.ACCEPTED) {

                // 3. 🔥 ПЕРЕНАПРАВЛЕНИЕ: Если отклик ПРИНЯТ, ищем Заказ
                Optional<Order> order = orderService.findByApplicationId(applicationId);

                if (order.isPresent()) {
                    log.info("Application {} is ACCEPTED. Redirecting Freelancer {} to OrderDetailsCommand.", applicationId, chatId);

                    // Передаем ID Заказа в OrderDetailsCommand
                    context.setParameter(order.get().getId().toString());
                    orderDetailsCommand.execute(context);

                } else {
                    // (Ошибка: отклик принят, но заказ не найден - такого быть не должно)
                    botExecutor.editMessageWithHtml(chatId, context.getMessageId(), "❌ Ошибка: Заказ не найден, хотя отклик принят.", null);
                }

            } else {

                // 4. СТАНДАРТНАЯ ЛОГИКА: Если PENDING или REJECTED, показываем детали ОТКЛИКА
                log.info("Application {} is {}. Showing Application details.", applicationId, application.getStatus());

                // --- Готовим текст и клавиатуру в зависимости от роли ---
                String text;
                InlineKeyboardMarkup keyboard;

                if (chatId.equals(freelancerChatId)) {
                    // --- ЛОГИКА ДЛЯ ИСПОЛНИТЕЛЯ ---
                    text = formatFreelancerApplicationDetails(application); // Переименовали старый метод
                    keyboard = applicationKeyboards.createApplicationDetailsKeyboard(
                            application.getId(),
                            chatId // Передаем chatId
                    );
                } else {
                    // --- ЛОГИКА ДЛЯ ЗАКАЗЧИКА ---
                    text = formatCustomerApplicationDetails(application); // 🔥 Новый метод
                    keyboard = applicationKeyboards.createApplicationDetailsKeyboard(
                            application.getId(),
                            chatId // Передаем chatId
                    );
                }


                botExecutor.editMessageWithHtml(chatId, messageId, text, keyboard);
            }




        } catch (Exception e) {
            log.error("❌ Ошибка показа деталей отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка загрузки информации об отклике", 5);
        }
    }

    /**
     * 🔥 СТАРЫЙ МЕТОД (переименован)
     * Форматирование деталей отклика для ИСПОЛНИТЕЛЯ
     */
    private String formatFreelancerApplicationDetails(ApplicationDto application) {
        // ... (весь твой код из formatApplicationDetails)
        ProjectDto project = application.getProject();

        return """
        <b>📋 **ДЕТАЛИ ВАШЕГО ОТКЛИКА**</b>

        <blockquote><b>💼 *Проект:* %s</b>
        <b>👔 *Заказчик:* @%s</b>
        <b>⭐ *Рейтинг заказчика:* %.1f/5.0</b>

        <b>💰 *Ваше предложение по бюджету:* %.0f руб</b>
        <b>💵 *Бюджет проекта:* %.0f руб</b>

        <b>⏱️ *Ваш срок выполнения:* %d дней</b>
        <b>📅 *Срок проекта:* %d дней</b>

        <b>📅 *Отклик отправлен:* %s</b>
        <b>📊 *Статус:* %s</b>
        <b>%s</b>
        <b>📝 *Ваше сопроводительное письмо:*</b>
        <i>%s</i>

        <b>🛠️ *Требуемые навыки:*</b>
        <u>%s</u></blockquote>
        """.formatted(
                project.getTitle(),
                project.getCustomerUserName() != null ? project.getCustomerUserName() : "скрыт",
                project.getCustomerRating(),
                application.getProposedBudget(),
                project.getBudget(),
                application.getProposedDays(),
                project.getEstimatedDays(),
                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                getApplicationStatusDisplay(application.getStatus()),
                getApplicationStatusDetails(application),
                application.getCoverLetter(),
                project.getRequiredSkills() != null ? project.getRequiredSkills() : "не указаны"
        );
    }

    /**
     * 🔥 НОВЫЙ МЕТОД
     * Форматирование деталей отклика для ЗАКАЗЧИКА
     */
    private String formatCustomerApplicationDetails(ApplicationDto application) {
        ProjectDto project = application.getProject();

        return """
        <b>📥 **ДЕТАЛИ ОТКЛИКА НА ВАШ ПРОЕКТ**</b>

        <blockquote><b>💼 *Проект:* %s</b>
        
        <b>👨‍💻 *Исполнитель:* @%s</b>
        <b>⭐ *Рейтинг:* %.1f/5.0</b>

        <b>💰 *Предложение по бюджету:* %.0f руб</b>
        (Бюджет проекта: %.0f руб)

        <b>⏱️ *Предлагаемый срок:* %d дней</b>
        (Срок проекта: %d дней)

        <b>📅 *Отправлен:* %s</b>
        <b>📊 *Статус:* %s</b>
        %s
        <b>📝 *Сопроводительное письмо:*</b>
        <i>%s</i>
        </blockquote>
        """.formatted(
                project.getTitle(),
                application.getFreelancer().getUserName() != null ? application.getFreelancer().getUserName() : "скрыт",
                application.getFreelancer().getRating(),
                application.getProposedBudget(),
                project.getBudget(),
                application.getProposedDays(),
                project.getEstimatedDays(),
                application.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                getApplicationStatusDisplay(application.getStatus()),
                getApplicationStatusDetails(application), // Этот метод можно переиспользовать
                application.getCoverLetter()
        );
    }

    private String getApplicationStatusDisplay(UserRole.ApplicationStatus applicationStatus) {
        return switch (applicationStatus) {
            case PENDING -> "Ожидает рассмотрения";
            case ACCEPTED -> "Принят заказчиком";
            case REJECTED -> "Отклонен заказчиком";
            case WITHDRAWN -> "Отозван исполнителем";
        };
    }

    // 🔥 ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ О СТАТУСЕ
    private String getApplicationStatusDetails(ApplicationDto application) {
        if (application.getReviewedAt() != null && application.getCustomerComment() != null) {
            return "💬 *Комментарий заказчика:* " + application.getCustomerComment() + "\n";
        }
        return "";
    }
}
