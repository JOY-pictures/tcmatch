package com.devlink.devlink.bot.keyboards;


import com.devlink.devlink.model.RegistrationStatus;
import com.devlink.devlink.service.ProjectSearchService;
import com.devlink.devlink.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyboardFactory {

    private final UserService userService;
    private final ProjectSearchService projectSearchService;

    public InlineKeyboardMarkup getKeyboardForUser(Long chatId) {

        if (!userService.userExists(chatId)) {
            return createUnauthorizedUserKeyboard();
        } else if (!userService.hasFullAccess(chatId)) {
            RegistrationStatus status = userService.getRegistrationStatus(chatId);
            return createRegistrationInProgressKeyboard(status);
        } else {
            return createMainMenuKeyboard();
        }
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

    public InlineKeyboardMarkup createSearchFiltersKeyboard(String currentFilter) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Фильтры
        List<InlineKeyboardButton> filterRow1 = new ArrayList<>();
        filterRow1.add(createFilterButton("Все", "", currentFilter));
        filterRow1.add(createFilterButton("До 10к", "budget:10000", currentFilter));
        filterRow1.add(createFilterButton("До 50к", "budget:50000", currentFilter));
        rows.add(filterRow1);

        List<InlineKeyboardButton> filterRow2 = new ArrayList<>();
        filterRow2.add(createFilterButton("Срочные", "urgent", currentFilter));
        filterRow2.add(createFilterButton("Без опыта", "junior", currentFilter));
        rows.add(filterRow2);

        // Кнопка поиска
        List<InlineKeyboardButton> searchRow = new ArrayList<>();
        searchRow.add(InlineKeyboardButton.builder()
                .text("🔍 Начать поиск")
                .callbackData("project:search")
                .build());
        rows.add(searchRow);


        // Назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back:main")
                .build());
        rows.add(backRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // Метод для клавиатуры пагинации
    public InlineKeyboardMarkup createPaginationKeyboard(String filter, Long chatId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Пагинация
        List<InlineKeyboardButton> paginationRow = new ArrayList<>();

        if (projectSearchService.hasPrevPage(chatId)) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("◀️ Пред.")
                    .callbackData("project:page:prev:" + filter)
                    .build());
        }

        paginationRow.add(InlineKeyboardButton.builder()
                .text("📄 " + (projectSearchService.getCurrentPage(chatId) + 1))
                .callbackData("project:page:current")
                .build());

        if (projectSearchService.hasNextPage(chatId)) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("След. ▶️")
                    .callbackData("project:page:next:" + filter)
                    .build());
        }

        rows.add(paginationRow);

        // Фильтры
        List<InlineKeyboardButton> filterRow = new ArrayList<>();
        filterRow.add(InlineKeyboardButton.builder()
                .text("⚙️ Фильтры")
                .callbackData("project:filters:" + filter)
                .build());
        rows.add(filterRow);

        // Назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад в меню")
                .callbackData("navigation:back:main")
                .build());
        rows.add(backRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // Метод для превью проекта
    public InlineKeyboardMarkup createProjectPreviewKeyboard(Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("📋 Детали")
                .callbackData("project:details:" + projectId)
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("✅ Откликнуться")
                .callbackData("project:apply:" + projectId)
                .build());
        rows.add(row);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private InlineKeyboardButton createFilterButton(String text, String filter, String currentFilter) {
        String prefix = filter.equals(currentFilter) ? "✅ " : "";
        return InlineKeyboardButton.builder()
                .text(prefix + text)
                .callbackData("project:filter:" + filter)
                .build();
    }

    public InlineKeyboardMarkup createActionWithBack(List<InlineKeyboardButton> action) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (action != null && !action.isEmpty()) {
            for (int i = 0; i < action.size(); i += 2) {
                List<InlineKeyboardButton> actionRow = new ArrayList<>();
                actionRow.add(action.get(i));
                if (i + 1 < action.size()) {
                    actionRow.add(action.get(i+1));
                }
                rows.add(actionRow);
            }
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

    public InlineKeyboardMarkup createUnauthorizedUserKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        row1.add(InlineKeyboardButton.builder()
                .text("Начать регистрацию")
                .callbackData("register:start")
                .build());
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();

        row2.add(InlineKeyboardButton.builder()
                .text("📋 Правила платформы")
                .callbackData("rules:preview")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("ℹ️ О проекте")
                .callbackData("menu:about")
                .build());
        rows.add(row2);
        inlineKeyboard.setKeyboard(rows);
        log.debug("✅ Unauthorized user keyboard created successfully");
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createRegistrationInProgressKeyboard(RegistrationStatus status) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        switch (status) {
            case NOT_REGISTERED:
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                row1.add(InlineKeyboardButton.builder()
                        .text("🚀 Начать регистрацию")
                        .callbackData("register:start")
                        .build());
                rows.add(row1);
                break;

            case REGISTERED:
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                row2.add(InlineKeyboardButton.builder()
                        .text("📜 Ознакомиться с правилами")
                        .callbackData("rules:view")
                        .build());
                rows.add(row2);
                break;

            case RULES_VIEWED:
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                row3.add(InlineKeyboardButton.builder()
                        .text("✅ Принять правила")
                        .callbackData("rules:accept")
                        .build());
                rows.add(row3);

//                List<InlineKeyboardButton> row4 = new ArrayList<>();
//                row4.add(InlineKeyboardButton.builder()
//                        .text("📜 Перечитать правила")
//                        .callbackData("rules:view")
//                        .build());
//                rows.add(row4);
                break;
            case RULES_ACCEPTED:
                // Если статус RULES_ACCEPTED - возвращаем главное меню
                return createMainMenuKeyboard();
        }

        // Кнопка помощи для всех статусов
        List<InlineKeyboardButton> helpRow = new ArrayList<>();
        helpRow.add(InlineKeyboardButton.builder()
                .text("❓ Помощь")
                .callbackData("menu:help")
                .build());
        rows.add(helpRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
        }

    public InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Мой профиль")
                .callbackData("menu:profile")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("💼 Проекты")
                .callbackData("menu:projects")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🚀 Создать проект")
                .callbackData("menu:create_project")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("👥 Найти исполнителей")
                .callbackData("menu:browse_freelancers")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("📊 Мои заказы")
                .callbackData("menu:my_orders")
                .build());
        row3.add(InlineKeyboardButton.builder()
                .text("ℹ️ Помощь")
                .callbackData("menu:help")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }


}
