package com.tcmatch.tcmatch.bot.keyboards;


import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ProjectSearchService;
import com.tcmatch.tcmatch.service.UserService;
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
            UserRole.RegistrationStatus status = userService.getRegistrationStatus(chatId);
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

//        // Кнопка поиска
//        List<InlineKeyboardButton> searchRow = new ArrayList<>();
//        searchRow.add(InlineKeyboardButton.builder()
//                .text("🔍 Начать поиск")
//                .callbackData("project:search")
//                .build());
//        rows.add(searchRow);


        // Назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
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
                .callbackData("projects:details:" + projectId)
                .build());

        rows.add(row);
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
                .callbackData("projects:filter:" + safeFilter)
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
                    actionRow.add(action.get(i + 1));
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

        inlineKeyboard.setKeyboard(rows);
        log.debug("✅ Unauthorized user keyboard created successfully");
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createRegistrationInProgressKeyboard(UserRole.RegistrationStatus status) {
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
                // 🔥 ДОБАВЛЯЕМ ЭТАП ВЫБОРА РОЛИ

                List<InlineKeyboardButton> roleRow1 = new ArrayList<>();
                roleRow1.add(InlineKeyboardButton.builder()
                        .text("👨‍💻 Я Исполнитель")
                        .callbackData("register:role:freelancer")
                        .build());

                List<InlineKeyboardButton> roleRow2 = new ArrayList<>();
                roleRow2.add(InlineKeyboardButton.builder()
                        .text("👔 Я Заказчик")
                        .callbackData("register:role:customer")
                        .build());

                rows.add(roleRow1);
                rows.add(roleRow2);
                break;

            case ROLE_SELECTED:
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

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createToMainMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDFE0Главный экран")
                .callbackData("menu:main")
                .build());
        rows.add(row1);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createPersonalAccountKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📊 Статистика")
                .callbackData("user_profile:statistics")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("✏️ Профиль")
                .callbackData("user_profile:edit")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);

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
                .text("👥 Исполнители")
                .callbackData("menu:freelancers")
                .build());

        row2.add(InlineKeyboardButton.builder()
                .text("ℹ️ Помощь")
                .callbackData("menu:help")
                .build());

        rows.add(row1);
        rows.add(row2);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createProjectsMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("❤️ Избранное")
                .callbackData("projects:favorites")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("📨 Отклики")
                .callbackData("projects:applications")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("⚙️ Выполняемые")
                .callbackData("projects:active")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск проектов")
                .callbackData("projects:filter")
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

    public InlineKeyboardMarkup createHelpMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📜 Правила")
                .callbackData("help:rules")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("ℹ️ Информация")
                .callbackData("help:info")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🛠️ Тех поддержка")
                .callbackData("help:support")
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

    public InlineKeyboardMarkup createFreelancersMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск")
                .callbackData("freelancers:search")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("⭐ Избранные")
                .callbackData("freelancers:favorites")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createSearchStartKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("🔍 Начать поиск")
                .callbackData("project_search:show")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("⚙️ Фильтры")
                .callbackData("project_search:filters")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("📋 Мои поиски")
                .callbackData("project_search:history")
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

    // 🔥 КЛАВИАТУРА УПРАВЛЕНИЯ ПОИСКОМ
    public InlineKeyboardMarkup createSearchControlKeyboard(String currentFilter) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 КНОПКИ ФИЛЬТРОВ
        List<InlineKeyboardButton> filterRow1 = new ArrayList<>();
        filterRow1.add(createFilterButton("Все", "all", currentFilter));
        filterRow1.add(createFilterButton("До 10к", "budget:10000", currentFilter));
        filterRow1.add(createFilterButton("До 50к", "budget:50000", currentFilter));

        List<InlineKeyboardButton> filterRow2 = new ArrayList<>();
        filterRow2.add(createFilterButton("Срочные", "urgent", currentFilter));
        filterRow2.add(createFilterButton("Без опыта", "junior", currentFilter));

        rows.add(filterRow1);
        rows.add(filterRow2);

        // 🔥 КНОПКИ УПРАВЛЕНИЯ
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад в меню")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }


    public InlineKeyboardMarkup createProjectDetailsKeyboard(Long projectId, boolean fromSearch) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("✅ Откликнуться")
                .callbackData("application:create:" + projectId)
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("⭐ В избранное")
                .callbackData("projects:favorite:" + projectId)
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("👔 Профиль заказчика")
                .callbackData("projects:customer:" + projectId)
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("💬 Задать вопрос")
                .callbackData("projects:question:" + projectId)
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


    public InlineKeyboardMarkup createApplyFormKeyboard(Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> applyRow = new ArrayList<>();
        applyRow.add(InlineKeyboardButton.builder()
                .text("📝 Написать отклик")
                .callbackData("application:create:" + projectId)
                .build());
        rows.add(applyRow);

        // 🔥 ДОПОЛНИТЕЛЬНЫЕ КНОПКИ
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();
        actionsRow.add(InlineKeyboardButton.builder()
                .text("⭐ В избранное")
                .callbackData("projects:favorite:" + projectId)
                .build());
        actionsRow.add(InlineKeyboardButton.builder()
                .text("👔 Профиль заказчика")
                .callbackData("projects:customer:" + projectId)
                .build());
        rows.add(actionsRow);

        // 🔥 КНОПКА НАЗАД К ПОИСКУ
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());
        rows.add(backRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }


    public InlineKeyboardMarkup createProjectWithPaginationKeyboard(Long projectId, int currentIndex, int total, String context) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Пагинация
        List<InlineKeyboardButton> paginationRow = new ArrayList<>();

        if (currentIndex > 0) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("◀️ Пред.")
                    .callbackData("projects:pagination:" + context + ":" + (currentIndex - 1))
                    .build());
        }

        paginationRow.add(InlineKeyboardButton.builder()
                .text((currentIndex + 1) + "/" + total)
                .callbackData("projects:current")
                .build());

        if (currentIndex < total - 1) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("След. ▶️")
                    .callbackData("projects:pagination:" + context + ":" + (currentIndex + 1))
                    .build());
        }

        rows.add(paginationRow);

        // Действия с проектом
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();
        actionsRow.add(InlineKeyboardButton.builder()
                .text("📋 Детали")
                .callbackData("projects:details:" + projectId)
                .build());
        actionsRow.add(InlineKeyboardButton.builder()
                .text("✅ Отклик")
                .callbackData("projects:apply:" + projectId)
                .build());

        rows.add(actionsRow);

        // Навигация
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("projects:" + context)
                .build());

        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createPaginationKeyboard(String currentFilter, Long chatId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        ProjectSearchService.SearchState state = projectSearchService.getOrCreateSearchState(chatId, currentFilter);
        int totalPages = (int) Math.ceil((double) state.projects.size() / state.pageSize);

        // 🔥 ПАГИНАЦИЯ
        List<InlineKeyboardButton> paginationRow = new ArrayList<>();

        if (projectSearchService.hasPrevPage(chatId)) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("◀️ Предыдущая")
                    .callbackData("projects:pagination:prev:" + currentFilter)
                    .build());
        }

        if (projectSearchService.hasNextPage(chatId)) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("Следующая ▶️")
                    .callbackData("projects:pagination:next:" + currentFilter)
                    .build());
        }

        rows.add(paginationRow);

        // 🔥 БЫСТРЫЕ ДЕЙСТВИЯ
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();
        actionsRow.add(InlineKeyboardButton.builder()
                .text("🗑️ Сбросить фильтры")
                .callbackData("projects:filter:")
                .build());

        rows.add(actionsRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createApplicationStepKeyboard(String step, Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка отмены
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(InlineKeyboardButton.builder()
                .text("❌ Отменить")
                .callbackData("application:cancel")
                .build());
        rows.add(cancelRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createSubscriptionKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> subscriptionRow = new ArrayList<>();
        subscriptionRow.add(InlineKeyboardButton.builder()
                .text("💎 Купить подписку")
                .callbackData("subscription:buy")
                .build());
        rows.add(subscriptionRow);

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("application:cancel")
                .build());
        rows.add(backRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ДЛЯ РЕЖИМА РЕДАКТИРОВАНИЯ
    public InlineKeyboardMarkup createApplicationEditStepKeyboard(String step, Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ТОЛЬКО КНОПКА ОТМЕНЫ РЕДАКТИРОВАНИЯ
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        cancelRow.add(InlineKeyboardButton.builder()
                .text("↩️ Отменить редактирование")
                .callbackData("application:edit_cancel")
                .build());
        rows.add(cancelRow);

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

    public InlineKeyboardMarkup createRoleSelectionKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("👔 Я Заказчик")
                .callbackData("register:role:customer")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("👨‍💻 Я Исполнитель")
                .callbackData("register:role:freelancer")
                .build());

        rows.add(row1);
        rows.add(row2);

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

    private InlineKeyboardMarkup createFreelancerProjectsMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("⚙️ Выполняемые")
                .callbackData("projects:active")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("❤️ Избранное")
                .callbackData("projects:favorites")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("📨 Откликнутые")
                .callbackData("projects:applications")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск проектов")
                .callbackData("projects:filter:")
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

    private InlineKeyboardMarkup createCustomerProjectsMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Мои проекты")
                .callbackData("customer_projects:menu")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text("❤️ Избранное")
                .callbackData("projects:favorites")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🔍 Поиск проектов")
                .callbackData("projects:search")
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

    // 🔥 КНОПКА НАЗАД К "МОИМ ПРОЕКТАМ"
    public InlineKeyboardMarkup createBackToMyProjectsKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад к Моим проектам")
                .callbackData("projects:my_projects")
                .build());

        rows.add(row);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА СПИСКА ПРОЕКТОВ ЗАКАЗЧИКА С ПАГИНАЦИЕЙ
    public InlineKeyboardMarkup createCustomerProjectsListKeyboard(List<Project> projects, int currentPage, int totalPages, String filter) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ПАГИНАЦИЯ
        if (totalPages > 1) {
            List<InlineKeyboardButton> paginationRow = new ArrayList<>();

            if (currentPage > 0) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text("◀️ Пред.")
                        .callbackData("projects:pagination:prev:my_list:" + filter)
                        .build());
            }

            if (currentPage < totalPages - 1) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text("След. ▶️")
                        .callbackData("projects:pagination:next:my_list:" + filter)
                        .build());
            }

            rows.add(paginationRow);
        }

        // 🔥 КНОПКИ ДЛЯ ПРОЕКТОВ ТЕКУЩЕЙ СТРАНИЦЫ
        int startIndex = currentPage * 3;
        int endIndex = Math.min(startIndex + 3, projects.size());

        for (int i = startIndex; i < endIndex; i++) {
            Project project = projects.get(i);
            List<InlineKeyboardButton> projectRow = new ArrayList<>();

            // 🔥 КНОПКА ПРОЕКТА
            projectRow.add(InlineKeyboardButton.builder()
                    .text("📋 " + (i + 1))
                    .callbackData("projects:details:" + project.getId())
                    .build());

            // 🔥 КНОПКА ОТКЛИКОВ (только для открытых проектов)
            if (project.getStatus() == UserRole.ProjectStatus.OPEN) {
                projectRow.add(InlineKeyboardButton.builder()
                        .text("📨 Отклики")
                        .callbackData("projects:applications:" + project.getId())
                        .build());
            }

            rows.add(projectRow);
        }

        // 🔥 ОСНОВНАЯ НАВИГАЦИЯ
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("projects:my_projects")
                .build());

        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ОТКЛИКОВ НА ПРОЕКТ
    public InlineKeyboardMarkup createProjectApplicationsKeyboard(Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 КНОПКИ ДЕЙСТВИЙ С ОТКЛИКАМИ
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();
        actionsRow.add(InlineKeyboardButton.builder()
                .text("👀 Просмотреть все")
                .callbackData("projects:view_all_applications:" + projectId)
                .build());
        actionsRow.add(InlineKeyboardButton.builder()
                .text("📊 Статистика откликов")
                .callbackData("projects:applications_stats:" + projectId)
                .build());

        // 🔥 НАВИГАЦИЯ
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад к проекту")
                .callbackData("projects:details:" + projectId)
                .build());

        rows.add(actionsRow);
        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КНОПКА НАЗАД К ПОИСКУ (для исполнителей)
    public InlineKeyboardMarkup createBackToSearchKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КНОПКА НАЗАД К ПРОЕКТАМ (общая)
    public InlineKeyboardMarkup createBackToProjectsKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder()
                .text("⬅️ В меню проектов")
                .callbackData("projects:menu")
                .build());

        rows.add(row);
        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА СПИСКА ОТКЛИКОВ ИСПОЛНИТЕЛЯ
    public InlineKeyboardMarkup createApplicationsListKeyboard(List<Application> applications, int currentPage, int totalPages) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ПАГИНАЦИЯ
        if (totalPages > 1) {
            List<InlineKeyboardButton> paginationRow = new ArrayList<>();

            if (currentPage > 0) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text("◀️ Пред.")
                        .callbackData("projects:pagination:applications:prev")
                        .build());
            }

            if (currentPage < totalPages - 1) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text("След. ▶️")
                        .callbackData("projects:pagination:applications:next")
                        .build());
            }

            rows.add(paginationRow);
        }

        // 🔥 КНОПКИ ДЛЯ ОТКЛИКОВ ТЕКУЩЕЙ СТРАНИЦЫ
        int startIndex = currentPage * 5;
        int endIndex = Math.min(startIndex + 5, applications.size());

        for (int i = startIndex; i < endIndex; i++) {
            Application app = applications.get(i);
            List<InlineKeyboardButton> applicationRow = new ArrayList<>();

            // 🔥 КНОПКА ПРОЕКТА
            applicationRow.add(InlineKeyboardButton.builder()
                    .text("📋 " + (i + 1))
                    .callbackData("projects:details:" + app.getProject().getId())
                    .build());

            // 🔥 КНОПКА ОТОЗВАТЬ (только для pending)
            if (app.getStatus() == UserRole.ApplicationStatus.PENDING) {
                applicationRow.add(InlineKeyboardButton.builder()
                        .text("↩️ Отозвать")
                        .callbackData("application:withdraw:" + app.getId())
                        .build());
            }

            rows.add(applicationRow);
        }

        // 🔥 ОСНОВНАЯ НАВИГАЦИЯ
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("🔍 Найти проекты")
                .callbackData("projects:search")
                .build());
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("projects:menu")
                .build());

        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ДЛЯ КАЖДОГО ОТКЛИКА
    public InlineKeyboardMarkup createApplicationItemKeyboard(Long applicationId, UserRole.ApplicationStatus status) {
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

    // 🔥 КЛАВИАТУРА УПРАВЛЕНИЯ ОТКЛИКАМИ
    public InlineKeyboardMarkup createApplicationsControlKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    // 🔥 КЛАВИАТУРА ПАГИНАЦИИ ДЛЯ ОТКЛИКОВ
    public InlineKeyboardMarkup createApplicationsPaginationKeyboard(int currentPage, int totalApplications) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        int pageSize = 5;
        int totalPages = (int) Math.ceil((double) totalApplications / pageSize);

        // 🔥 ПАГИНАЦИЯ
        List<InlineKeyboardButton> paginationRow = new ArrayList<>();

        if (currentPage > 0) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("◀️ Предыдущая")
                    .callbackData("projects:pagination:applications:prev")
                    .build());
        }

        if (currentPage < totalPages - 1) {
            paginationRow.add(InlineKeyboardButton.builder()
                    .text("Следующая ▶️")
                    .callbackData("projects:pagination:applications:next")
                    .build());
        }

        if (!paginationRow.isEmpty()) {
            rows.add(paginationRow);
        }

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public InlineKeyboardMarkup createApplicationDetailsKeyboard (Long applicationId, UserRole.ApplicationStatus status) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ОСНОВНЫЕ ДЕЙСТВИЯ
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();

        // 🔥 КНОПКА "ПРОЕКТ" - ВЕРНУТЬСЯ К ПРОЕКТУ
        actionsRow.add(InlineKeyboardButton.builder()
                .text("📋 К проекту")
                .callbackData("projects:details:app_" + applicationId) // Будет искать проект по applicationId
                .build());

        // 🔥 КНОПКА "ОТОЗВАТЬ" (только для pending)
        if (status == UserRole.ApplicationStatus.PENDING) {
            actionsRow.add(InlineKeyboardButton.builder()
                    .text("↩️ Отозвать")
                    .callbackData("application:confirm_withdraw:" + applicationId)
                    .build());
        }

        rows.add(actionsRow);

        // 🔥 ДОПОЛНИТЕЛЬНЫЕ ДЕЙСТВИЯ
        List<InlineKeyboardButton> additionalRow = new ArrayList<>();
        additionalRow.add(InlineKeyboardButton.builder()
                .text("👔 Профиль заказчика")
                .callbackData("projects:customer_from_application:" + applicationId)
                .build());

        rows.add(additionalRow);

        // 🔥 НАВИГАЦИЯ
        List<InlineKeyboardButton> navRow = new ArrayList<>();
        navRow.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(navRow);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

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
}
