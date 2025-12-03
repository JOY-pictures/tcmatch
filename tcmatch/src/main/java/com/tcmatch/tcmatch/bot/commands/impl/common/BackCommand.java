package com.tcmatch.tcmatch.bot.commands.impl.common;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.dispatcher.CommandDispatcher;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BackCommand implements Command {

    private final UserSessionService userSessionService;
    private final BotExecutor botExecutor;
    private final CommonKeyboards commonKeyboards;
    private final TextMessageService textMessageService;

    @Lazy
    @Autowired
    private CommandDispatcher commandDispatcher;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "navigation".equals(actionType) && "back".equals(action);
    }

    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        Integer messageId = context.getMessageId();
        String userName = context.getUserName();

        // 1. Сохраняем ключ текущего экрана ПЕРЕД тем, как его заменить
        String currentScreen = userSessionService.getFromContext(chatId, "currentScreen", String.class);

        // 2. Получаем предыдущий экран из стека
        String previousScreen = userSessionService.popFromNavigationHistory(chatId);
        log.info("📱 Navigation back: {} -> {}", chatId, previousScreen);

        // 3. 🔥 ОЧИСТКА КОНТЕКСТА ТЕКУЩЕГО ЭКРАНА
        if (currentScreen != null && !currentScreen.trim().isEmpty()) {
            userSessionService.remove(chatId, currentScreen);
            log.debug("🗑️ Removed context data for screen: {}", currentScreen);
        }

        // 🔥 ОЧИЩАЕМ ВРЕМЕННЫЕ СООБЩЕНИЯ С ПРОЕКТАМИ ПЕРЕД НАВИГАЦИЕЙ
        if (!userSessionService.getTemporaryMessageIds(chatId).isEmpty()) {
            botExecutor.deletePreviousMessages(chatId);
        }

        // 🔥 ЕСЛИ ИСТОРИЯ ПУСТАЯ - ВОЗВРАЩАЕМ В ГЛАВНОЕ МЕНЮ
        if (previousScreen == null) {
            userSessionService.putToContext(chatId, "currentScreen", "main:menu");
            showMainMenu(chatId);
            return;
        }

        // 🔥 ОБНОВЛЯЕМ ТЕКУЩИЙ ЭКРАН В КОНТЕКСТЕ НА ТОТ, В КОТОРЫЙ ВОЗВРАЩАЕМСЯ
        userSessionService.putToContext(chatId, "currentScreen", previousScreen);
        log.debug("📱 Updated current screen after back navigation: {}", previousScreen);

        navigateToScreen(chatId, previousScreen, messageId, userName);
    }



    private void showMainMenu(Long chatId) {
        try {
            String text = textMessageService.getMainMenuText();

            InlineKeyboardMarkup keyboard = commonKeyboards.createMainMenuKeyboard(chatId);

            Integer mainMessageId = botExecutor.getOrCreateMainMessageId(chatId);
            botExecutor.editMessageWithHtml(chatId, mainMessageId, text, keyboard);

        } catch (Exception e) {
            log.error("❌ Error showing main menu for user {}: {}", chatId, e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка при открытии главного меню", 5);
        }
    }

    private void navigateToScreen(Long chatId, String screen, Integer messageId, String userName) {
        String[] parts = screen.split(":");
        String actionType = parts[0];
        String action = parts[1];
        String parameter = parts.length > 2 ? parts[2] : null;

        // 🔥 РЕДИРЕКТ
        commandDispatcher.handleCallback(
                chatId,
                screen,
                messageId,
                userName
        );

//        // 🔥 ИЩЕМ КОМАНДУ ДЛЯ ВОССТАНОВЛЕНИЯ ЭКРАНА
//        boolean commandFound = false;
//        for (Command command : commands) {
//            // Пропускаем сам BackCommand чтобы избежать рекурсии
//            if (command instanceof BackCommand) {
//                continue;
//            }
//
//            if (command.canHandle(actionType, action)) {
//                log.info("✅ Restoring screen: {}:{}:{}", actionType, action, parameter);
//
//                // Создаем контекст для восстановления экрана
//                CommandContext context = new CommandContext(
//                        chatId, action, parameter, messageId, null, actionType
//                );
//
//                command.execute(context);
//                commandFound = true;
//                break;
//            }
//        }
//
//        if (!commandFound) {
//            log.warn("❌ Cannot restore screen: {}", screen);
//            showMainMenu(chatId);
//        }
    }
}
