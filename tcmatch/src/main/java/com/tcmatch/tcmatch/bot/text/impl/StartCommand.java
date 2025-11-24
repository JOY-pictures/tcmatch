package com.tcmatch.tcmatch.bot.text.impl;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.text.TextCommand;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartCommand implements TextCommand {

    private final List<Command> commands;
    private final BotExecutor botExecutor;
    private final UserSessionService userSessionService;
    private final CommonKeyboards commonKeyboards;
    private final UserService userService;
    private final TextMessageService textMessageService;

    @Override
    public boolean canHandle(Long chatId, String text) {
        return text.startsWith("/start");
    }

    @Override
    public void execute(Message message) {
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();

        try {

            // 1. Удаляем сообщение с командой /start
            botExecutor.deleteMessage(chatId, messageId);

            // 2. Проверяем наличие пользователя в БД
            boolean userExists = userService.userExists(chatId);

            // 3. Очищаем сессию если пользователь уже существует
            if (userExists) {
                if (userSessionService.hasSession(chatId)) {
                    log.info("🔄 Пользователь {} возвращается, очищаем сессию...", chatId);
                    userSessionService.resetToMain(chatId);
                    String menuText = textMessageService.getMainMenuText();
                    botExecutor.editMessageWithHtml(chatId, messageId, menuText, commonKeyboards.createMainMenuKeyboard());
                    return;
                }
            }

            // 4. Получаем приветственный текст (без имени)
            String welcomeText = textMessageService.getWelcomeText(chatId, message.getFrom().getFirstName());

            // 5. Создаем клавиатуру для пользователя
            InlineKeyboardMarkup keyboard = commonKeyboards.getKeyboardForUser(chatId);

            userSessionService.setMainMessageId(chatId, botExecutor.sendHtmlMessageReturnId(chatId, welcomeText, keyboard));

            // 8. Регистрируем/обновляем пользователя
            userService.registerFromTelegram(chatId, message.getFrom().getUserName(), message.getFrom().getFirstName(), message.getFrom().getLastName());

            log.info("✅ Пользователь {} {} начал работу с ботом",
                    chatId, userExists ? "возобновил" : "начал");

        } catch (Exception e) {
            log.error("❌ Error handling /start command for user {}: {}", chatId, e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка при запуске бота", 5);
        }
    }
}
