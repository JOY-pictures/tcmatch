package com.tcmatch.tcmatch.bot.keyboards;

import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.SearchRequest;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectKeyboards {

    private final UserService userService;
    private final ProjectService projectService;

    // Метод для превью проекта
    public InlineKeyboardMarkup createProjectPreviewKeyboard(Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("📋 Детали")
                .callbackData("project:details:" + projectId)
                .build());

        rows.add(row);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createProjectDetailsKeyboard(Long chatId, Long projectId, boolean canApply) {
        User user = userService.findByChatId(chatId).orElseThrow();

        return switch (user.getRole()) {
            case CUSTOMER -> createCustomerProjectDetailsKeyboard(chatId, projectId);
            case FREELANCER -> createFreelancerProjectDetailsKeyboard(projectId, canApply, chatId);
            default -> createBackButton();
        };
    }

    private InlineKeyboardMarkup createCustomerProjectDetailsKeyboard(Long chatId, Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        ProjectDto project = projectService.getProjectDtoById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));

        Long customerChatId = project.getCustomerChatId();
        boolean isCreator = project.getCustomerChatId().equals(chatId);

        if (isCreator) {

            if (project.getStatus().equals(UserRole.ProjectStatus.OPEN)) {

                // 🔥 ДЕТАЛИ ПРОЕКТА - ЗАКАЗЧИК (только нужные кнопки)
                List<InlineKeyboardButton> applicationsRow = new ArrayList<>();
                applicationsRow.add(InlineKeyboardButton.builder()
                        .text("📨 Отклики")
                        .callbackData("application:show_applications:" + projectId)
                        .build());
                rows.add(applicationsRow);

                List<InlineKeyboardButton> closeRaw = new ArrayList<>();
                closeRaw.add(InlineKeyboardButton.builder()
                        .text("🚫 Закрыть проект")
                        .callbackData("project:confirm_withdraw:" + projectId)
                        .build());
                rows.add(closeRaw);
            }
        } else {
            boolean isFavorite = userService.isProjectFavorite(chatId, projectId);
            List<InlineKeyboardButton> infRow = new ArrayList<>();
            if (isFavorite) {
                infRow.add(InlineKeyboardButton.builder()
                        .text("\uD83C\uDF1F Удалить из избранного")
                        .callbackData("project:favorite:remove:" + projectId)
                        .build());
            } else {
                infRow.add(InlineKeyboardButton.builder()
                        .text("⭐ Добавить в избранное")
                        .callbackData("project:favorite:add:" + projectId)
                        .build());
            }
            infRow.add(InlineKeyboardButton.builder()
                    .text("👔 Профиль заказчика")
                    .callbackData("profile:show_customer:" + customerChatId)
                    .build());
            rows.add(infRow);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(backRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;

    }

    private InlineKeyboardMarkup createFreelancerProjectDetailsKeyboard(Long projectId, boolean canApply, Long chatId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        ProjectDto project = projectService.getProjectDtoById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));
        Long customerChatId = project.getCustomerChatId();


        // Проверяем, находится ли проект уже в избранном у пользователя
        boolean isFavorite = userService.isProjectFavorite(chatId, projectId);

        // 🔥 ДЕТАЛИ ПРОЕКТА - ИСПОЛНИТЕЛЬ
        if (canApply) {
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            row1.add(InlineKeyboardButton.builder()
                    .text("✅ Откликнуться")
                    .callbackData("application:create:" + projectId)
                    .build());
            rows.add(row1);
        }

        List<InlineKeyboardButton> row2 = new ArrayList<>();

        if (isFavorite) {
            row2.add(InlineKeyboardButton.builder()
                    .text("\uD83C\uDF1F Удалить из избранного")
                    .callbackData("project:favorite:remove:" + projectId)
                    .build());
        } else {
            row2.add(InlineKeyboardButton.builder()
                    .text("⭐ Добавить в избранное")
                    .callbackData("project:favorite:add:" + projectId)
                    .build());
        }
        row2.add(InlineKeyboardButton.builder()
                .text("👔 Профиль заказчика")
                .callbackData("profile:show_customer:" + customerChatId)
                .build());
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private InlineKeyboardButton createFilterButton(String text, String filter, String currentFilter) {
        // Убедитесь, что currentFilter не null
        String safeCurrentFilter = currentFilter != null ? currentFilter : "";
        String safeFilter = filter != null ? filter : "";

        // Сравниваем фильтры - добавляем ✅ если они совпадаютp
        boolean isActive = safeFilter.equals(safeCurrentFilter);
        String buttonText = (isActive ? "✅ " : "") + text;

        return InlineKeyboardButton.builder()
                .text(buttonText)
                .callbackData("project:filter:" + safeFilter)
                .build();
    }

    public InlineKeyboardMarkup createFilterSelectionKeyboard(SearchRequest request) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Проверяем бюджетный фильтр
        boolean isUnder10k = request.getMinBudget() != null && request.getMinBudget() <= 10000;
        boolean isUnder50k = request.getMinBudget() != null && request.getMinBudget() <= 50000 && !isUnder10k;

        List<InlineKeyboardButton> filterRaw = new ArrayList<>();

        // Кнопка "Все проекты"
        String allText = (request.isEmpty() ? "✅ " : "") + "Все проекты";

        InlineKeyboardButton allFilter = new InlineKeyboardButton();
        allFilter.setText(allText);
        allFilter.setCallbackData("project:filter:clear");
        filterRaw.add(allFilter);

        // Кнопка "До 10000"
        String b10kText = (isUnder10k ? "✅ " : "") + "От 10к";
        InlineKeyboardButton budgetFilter10k = new InlineKeyboardButton();
        budgetFilter10k.setText(b10kText);
        budgetFilter10k.setCallbackData("project:filter:budget:10000");
        filterRaw.add(budgetFilter10k);

        // Кнопка "До 50000"
        String b50kText = (isUnder50k ? "✅ " : "") + "От 50к";
        InlineKeyboardButton budgetFilter50k = new InlineKeyboardButton();
        budgetFilter50k.setText(b50kText);
        budgetFilter50k.setCallbackData("project:filter:budget:50000");
        filterRaw.add(budgetFilter50k);

        // ... другие фильтры

        // Кнопка "Начать поиск" (вызывает project:filter:apply)
        List<InlineKeyboardButton> applyRow =new ArrayList<>();
        InlineKeyboardButton apply = new InlineKeyboardButton();
        apply.setText("🚀 Начать поиск");
        apply.setCallbackData("project:filter:apply");
        applyRow.add(apply);

        List<InlineKeyboardButton> backRow =new ArrayList<>();
        InlineKeyboardButton back = new InlineKeyboardButton();
        back.setText("Назад");
        back.setCallbackData("navigation:back");
        backRow.add(back);

        keyboard.add(filterRaw);
        keyboard.add(applyRow);
        keyboard.add(backRow);

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup createProjectWithdrawConfirmationKeyboard(Long projectId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первый ряд - подтверждение удаления
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🗑️ Да, отменить проект")
                .callbackData("project:withdraw:" + projectId)
                .build());

        // Второй ряд - отмена
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("↩️ Отменить")
                .callbackData("project:details:" + projectId)
                .build());

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    // 🔥 КЛАВИАТУРА ПОДТВЕРЖДЕНИЯ СОЗДАНИЯ ПРОЕКТА
    public InlineKeyboardMarkup createProjectConfirmationKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 КНОПКИ РЕДАКТИРОВАНИЯ КАЖДОГО ПОЛЯ
        List<InlineKeyboardButton> editRow1 = new ArrayList<>();
        editRow1.add(InlineKeyboardButton.builder()
                .text("✏️ Название")
                .callbackData("project:edit_field:title")
                .build());
        editRow1.add(InlineKeyboardButton.builder()
                .text("📝 Описание")
                .callbackData("project:edit_field:description")
                .build());

        List<InlineKeyboardButton> editRow2 = new ArrayList<>();
        editRow2.add(InlineKeyboardButton.builder()
                .text("💰 Бюджет")
                .callbackData("project:edit_field:budget")
                .build());
        editRow2.add(InlineKeyboardButton.builder()
                .text("⏱️ Срок")
                .callbackData("project:edit_field:deadline")
                .build());

        List<InlineKeyboardButton> editRow3 = new ArrayList<>();
        editRow3.add(InlineKeyboardButton.builder()
                .text("🛠️ Навыки")
                .callbackData("project:edit_field:skills")
                .build());

        // 🔥 КНОПКИ ОСНОВНЫХ ДЕЙСТВИЙ
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        actionRow.add(InlineKeyboardButton.builder()
                .text("✅ Создать проект")
                .callbackData("project:confirm")
                .build());
        actionRow.add(InlineKeyboardButton.builder()
                .text("❌ Отменить")
                .callbackData("project:cancel_creation")
                .build());

        rows.add(editRow1);
        rows.add(editRow2);
        rows.add(editRow3);
        rows.add(actionRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ДЛЯ РЕДАКТИРОВАНИЯ ПОЛЯ ПРОЕКТА
    public InlineKeyboardMarkup createProjectEditKeyboard(String field) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ТОЛЬКО КНОПКА ОТМЕНЫ РЕДАКТИРОВАНИЯ
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(InlineKeyboardButton.builder()
                .text("↩️ Назад к подтверждению")
                .callbackData("project:edit_cancel")
                .build());
        rows.add(cancelRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ДЛЯ ПРОЦЕССА СОЗДАНИЯ ПРОЕКТА (ТОЛЬКО ОТМЕНА)
    public InlineKeyboardMarkup createProjectCreationKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ТОЛЬКО КНОПКА ОТМЕНЫ СОЗДАНИЯ
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(InlineKeyboardButton.builder()
                .text("❌ Отменить создание")
                .callbackData("project:cancel_creation")
                .build());
        rows.add(cancelRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private InlineKeyboardMarkup createFreelancerProjectsMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("⚙️ Выполняемые")
                .callbackData("application:accepted")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("⭐ Избранное")
                .callbackData("project:favorites")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("📨 Отклики")
                .callbackData("application:menu")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск проектов")
                .callbackData("project:filter:")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 РАЗНЫЕ МЕНЮ ПРОЕКТОВ ДЛЯ РАЗНЫХ РОЛЕЙ
    public InlineKeyboardMarkup createProjectsMenuKeyboard(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();

        if (user.getRole() == UserRole.CUSTOMER) {
            return createCustomerProjectsMenuKeyboard();
        } else {
            return createFreelancerProjectsMenuKeyboard();
        }
    }

    public InlineKeyboardMarkup createCustomerProjectsMainKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка создания проекта
        List<InlineKeyboardButton> createRow = new ArrayList<>();
        createRow.add(InlineKeyboardButton.builder()
                .text("➕ Создать проект")
                .callbackData("project:create")
                .build());
        rows.add(createRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private InlineKeyboardMarkup createCustomerProjectsMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Мои проекты")
                .callbackData("project:my_list")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("⭐ Избранное")
                .callbackData("project:favorites")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск проектов")
                .callbackData("project:filter:")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createMyProjectsMenu() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> createRow = new ArrayList<>();
        createRow.add(InlineKeyboardButton.builder()
                .text("➕ Создать проект")
                .callbackData("project:create")
                .build());
        rows.add(createRow);

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
