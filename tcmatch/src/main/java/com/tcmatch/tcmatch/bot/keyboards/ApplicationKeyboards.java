package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationKeyboards {

    private final ApplicationService applicationService;
    private final ProjectService projectService;

    public InlineKeyboardMarkup createWithdrawConfirmationKeyboard(Long applicationId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 КНОПКИ ПОДТВЕРЖДЕНИЯ
        List<InlineKeyboardButton> confirmRow1 = new ArrayList<>();
        confirmRow1.add(InlineKeyboardButton.builder()
                .text("✅ Да, отозвать")
                .callbackData("application:withdraw:" + applicationId)
                .build());

        List<InlineKeyboardButton> confirmRow2 = new ArrayList<>();
        confirmRow2.add(InlineKeyboardButton.builder()
                .text("❌ Нет, оставить")
                .callbackData("navigation:back")
                .build());

        rows.add(confirmRow1);
        rows.add(confirmRow2);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    /**
     * 🔥 ГЛАВНЫЙ МЕТОД-РОУТЕР
     * Он сам определит, кто смотрит, и вернет нужную клавиатуру.
     */
    public InlineKeyboardMarkup createApplicationDetailsKeyboard(Long applicationId, Long currentChatId) {

        ApplicationDto application = applicationService.getApplicationDtoById(applicationId);
        Long customerChatId = projectService.getCustomerChatIdByProjectId(application.getProjectId());
        Long freelancerChatId = application.getFreelancer().getChatId(); // ID исполнителя

        if (currentChatId.equals(freelancerChatId)) {
            // --- Пользователь - ИСПОЛНИТЕЛЬ ---
            return createFreelancerDetailsKeyboard(application);
        } else if (currentChatId.equals(customerChatId)) {
            // --- Пользователь - ЗАКАЗЧИК ---
            return createCustomerDetailsKeyboard(application);
        } else {
            // --- Неизвестный пользователь (на всякий случай) ---
            return createBackButton(); // Простая кнопка "Назад"
        }
    }

    /**
     * 🔥 НОВАЯ КЛАВИАТУРА ДЛЯ ЗАКАЗЧИКА
     * (С кнопками Принять/Отклонить)
     */
    private InlineKeyboardMarkup createCustomerDetailsKeyboard(ApplicationDto application) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 Кнопки "Принять" и "Отклонить" (только если отклик ожидает)
        if (application.getStatus() == UserRole.ApplicationStatus.PENDING) {
            List<InlineKeyboardButton> actionsRow = new ArrayList<>();
            actionsRow.add(InlineKeyboardButton.builder()
                    .text("✅ Принять")
                    .callbackData("application:accept:" + application.getId())
                    .build());
            actionsRow.add(InlineKeyboardButton.builder()
                    .text("❌ Отклонить")
                    .callbackData("application:reject:" + application.getId())
                    .build());
            rows.add(actionsRow);
        }

        // --- Дополнительные кнопки ---
        List<InlineKeyboardButton> additionalRow = new ArrayList<>();
        additionalRow.add(InlineKeyboardButton.builder()
                .text("👔 Профиль исполнителя")
                .callbackData("profile:show_freelancer:" + application.getFreelancer().getChatId())
                .build());
        additionalRow.add(InlineKeyboardButton.builder()
                .text("📋 К проекту")
                .callbackData("project:details:" + application.getProjectId())
                .build());
        rows.add(additionalRow);

        // --- Навигация ---
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());
        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }


    /**
     * 🔥 СТАРАЯ ЛОГИКА (теперь в отдельном методе)
     * Клавиатура для Исполнителя (с кнопкой "Отозвать")
     */
    private InlineKeyboardMarkup createFreelancerDetailsKeyboard(ApplicationDto application) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        Long customerChatId = projectService.getCustomerChatIdByProjectId(application.getProjectId());
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // --- Основные действия ---
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();
        actionsRow.add(InlineKeyboardButton.builder()
                .text("📋 К проекту")
                .callbackData("project:details:" + application.getProjectId())
                .build());

        // Кнопка "Отозвать" (только для PENDING)
        if (application.getStatus() == UserRole.ApplicationStatus.PENDING) {
            actionsRow.add(InlineKeyboardButton.builder()
                    .text("↩️ Отозвать")
                    .callbackData("application:confirm_withdraw:" + application.getId())
                    .build());
        }
        rows.add(actionsRow);

        // --- Дополнительные действия ---
        List<InlineKeyboardButton> additionalRow = new ArrayList<>();
        additionalRow.add(InlineKeyboardButton.builder()
                .text("👔 Профиль заказчика")
                .callbackData("profile:show_customer:" + customerChatId)
                .build());
        rows.add(additionalRow);

        // --- Навигация ---
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());
        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ДЛЯ КАЖДОГО ОТКЛИКА
    public InlineKeyboardMarkup createApplicationItemKeyboard(Long applicationId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        // 🔥 КНОПКА "ДЕТАЛИ ПРОЕКТА"
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Детали отклика")
                .callbackData("application:details:" + applicationId)
                .build());

        rows.add(row1);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createApplicationEditKeyboard(String field, Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ТОЛЬКО КНОПКА ОТМЕНЫ РЕДАКТИРОВАНИЯ (ВОЗВРАТ К ПОДТВЕРЖДЕНИЮ)
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(InlineKeyboardButton.builder()
                .text("↩️ Назад к подтверждению")
                .callbackData("application:edit_cancel")
                .build());
        rows.add(cancelRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createApplicationConfirmationKeyboard(Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 КНОПКИ РЕДАКТИРОВАНИЯ КАЖДОГО ПОЛЯ
        List<InlineKeyboardButton> editRow1 = new ArrayList<>();
        editRow1.add(InlineKeyboardButton.builder()
                .text("✏️ Описание")
                .callbackData("application:edit_field:description")
                .build());
        editRow1.add(InlineKeyboardButton.builder()
                .text("💰 Бюджет")
                .callbackData("application:edit_field:budget")
                .build());

        editRow1.add(InlineKeyboardButton.builder()
                .text("⏱️ Сроки")
                .callbackData("application:edit_field:deadline")
                .build());

        rows.add(editRow1);

        // 🔥 КНОПКИ ОСНОВНЫХ ДЕЙСТВИЙ
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        actionRow.add(InlineKeyboardButton.builder()
                .text("✅ Отправить")
                .callbackData("application:confirm")
                .build());
        actionRow.add(InlineKeyboardButton.builder()
                .text("❌ Отменить")
                .callbackData("application:cancel")
                .build());
        rows.add(actionRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createApplicationProcessKeyboard(String step, Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ТОЛЬКО КНОПКА ОТМЕНЫ - БЕЗ ВОЗМОЖНОСТИ РЕДАКТИРОВАНИЯ ОТДЕЛЬНЫХ ПОЛЕЙ
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(InlineKeyboardButton.builder()
                .text("❌ Отменить отклик")
                .callbackData("application:cancel")
                .build());
        rows.add(cancelRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createBackButton() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back") // Универсальный callback для возврата
                .build());
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

}
