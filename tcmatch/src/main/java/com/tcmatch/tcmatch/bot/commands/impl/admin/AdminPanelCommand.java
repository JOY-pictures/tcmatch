package com.tcmatch.tcmatch.bot.commands.impl.admin;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.AdminKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminPanelCommand implements Command {

    private final BotExecutor botExecutor;
    private final AdminService adminService;
    private final AdminKeyboards adminKeyboards;
    private final CommonKeyboards commonKeyboards;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "admin".equals(actionType) && "panel".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();

        // 🔥 ПРОВЕРКА ПРАВ ДОСТУПА
        if (!adminService.isAdmin(chatId)) {
            log.warn("Несанкционированный доступ к админ-панели: {}", chatId);

            botExecutor.sendTemporaryErrorMessageWithHtml(
                    chatId,
                    "⛔ У вас нет доступа к админ-панели",
                    5
            );
            return;
        }

        try {
            // Получаем или создаем главное сообщение
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

            // Удаляем предыдущие сообщения для чистоты
            botExecutor.deletePreviousMessages(chatId);

            // Формируем приветствие
            String greeting = formatAdminGreeting(chatId, adminService.isSuperAdmin(chatId));

            // Получаем клавиатуру админа
            InlineKeyboardMarkup keyboard = adminKeyboards.createMainAdminMenu(chatId);

            // Отправляем/редактируем сообщение
            botExecutor.editMessageWithHtml(chatId, messageId, greeting, keyboard);

            log.info("Админ {} вошел в админ-панель", chatId);

        } catch (Exception e) {
            log.error("Ошибка при открытии админ-панели: {}", e.getMessage(), e);
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки админ-панели", 5);
        }
    }

    /**
     * Форматирует приветствие для админа
     */
    private String formatAdminGreeting(Long chatId, boolean isSuperAdmin) {
        String role = isSuperAdmin ? "👑 Супер-админ" : "🛡️ Администратор";

        return """
        <b>%s Панель управления</b>
        
        🔸 <b>Статус:</b> Активен
        🔸 <b>Роль:</b> %s
        
        <b>Выберите раздел:</b>
        """.formatted("🛠️", role);
    }
}