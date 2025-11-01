package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoleBasedMenuService {

    private final UserService userService;
    private final KeyboardFactory keyboardFactory;

    // 🔥 ГЛАВНОЕ МЕНЮ ПРОЕКТОВ ПО РОЛИ
    public InlineKeyboardMarkup createProjectsMenu(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();

        return switch (user.getRole()) {
            case CUSTOMER -> createCustomerProjectsMenu();
            case FREELANCER -> createFreelancerProjectsMenu();
            default -> createDefaultProjectsMenu();
        };
    }

    // 🔥 МЕНЮ "МОИ ПРОЕКТЫ" - ТОЛЬКО ДЛЯ ЗАКАЗЧИКОВ
    public InlineKeyboardMarkup createMyProjectsMenu(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();

        if (user.getRole() == UserRole.CUSTOMER) {
            return createCustomerMyProjectsMenu();
        } else {
            // 🔥 ИСПОЛНИТЕЛЯМ ПОКАЗЫВАЕМ, ЧТО РАЗДЕЛ НЕ ДОСТУПЕН
            return createNotAvailableForFreelancerKeyboard();
        }
    }

    // 🔥 КЛАВИАТУРА ДЕТАЛЕЙ ПРОЕКТА ПО РОЛИ
    public InlineKeyboardMarkup createProjectDetailsKeyboard(Long chatId, Long projectId, boolean canApply) {
        User user = userService.findByChatId(chatId).orElseThrow();

        return switch (user.getRole()) {
            case CUSTOMER -> createCustomerProjectDetailsKeyboard(projectId);
            case FREELANCER -> createFreelancerProjectDetailsKeyboard(projectId, canApply);
            default -> keyboardFactory.createBackButton();
        };
    }

    // 🔥 ПРОВЕРКИ ДОСТУПА ПО РОЛЯМ
    public boolean canUserApplyToProjects(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole() == UserRole.FREELANCER;
    }

    public boolean canUserCreateProjects(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole() == UserRole.CUSTOMER;
    }

    public boolean isProjectOwner(Long chatId, Long projectCustomerId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole() == UserRole.CUSTOMER &&
                user.getId().equals(projectCustomerId);
    }

    public UserRole getUserRole(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole();
    }

    // 🔥 ПРИВАТНЫЕ МЕТОДЫ ДЛЯ КАЖДОЙ РОЛИ

    private InlineKeyboardMarkup createCustomerProjectsMenu() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 МЕНЮ ЗАКАЗЧИКА
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Мои проекты")
                .callbackData("projects:my_projects")
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

    private InlineKeyboardMarkup createFreelancerProjectsMenu() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 МЕНЮ ИСПОЛНИТЕЛЯ
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

    private InlineKeyboardMarkup createCustomerMyProjectsMenu() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 МОИ ПРОЕКТЫ - ЗАКАЗЧИК
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📋 Все проекты")
                .callbackData("projects:my_list:all")
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🔓 Открытые")
                .callbackData("projects:my_list:open")
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("⚙️ В работе")
                .callbackData("projects:my_list:in_progress")
                .build());

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(InlineKeyboardButton.builder()
                .text("✅ Завершенные")
                .callbackData("projects:my_list:completed")
                .build());
        row3.add(InlineKeyboardButton.builder()
                .text("➕ Создать")
                .callbackData("project_creation:start")
                .build());

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(InlineKeyboardButton.builder()
                .text("⬅️ Назад")
                .callbackData("navigation:back")
                .build());

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private InlineKeyboardMarkup createCustomerProjectDetailsKeyboard(Long projectId) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 🔥 ДЕТАЛИ ПРОЕКТА - ЗАКАЗЧИК (только нужные кнопки)
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder()
                .text("📨 Отклики")
                .callbackData("projects:applications:" + projectId)
                .build());

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("🚫 Закрыть проект")
                .callbackData("projects:close:" + projectId)
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

    private InlineKeyboardMarkup createFreelancerProjectDetailsKeyboard(Long projectId, boolean canApply) {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

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
        row2.add(InlineKeyboardButton.builder()
                .text("⭐ В избранное")
                .callbackData("projects:favorite:" + projectId)
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("👔 Профиль заказчика")
                .callbackData("projects:customer:" + projectId)
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

    private InlineKeyboardMarkup createNotAvailableForFreelancerKeyboard() {
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

    private InlineKeyboardMarkup createDefaultProjectsMenu() {
        return keyboardFactory.createBackButton();
    }
}
