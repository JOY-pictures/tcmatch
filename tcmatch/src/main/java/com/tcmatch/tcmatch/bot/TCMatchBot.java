package com.tcmatch.tcmatch.bot;

import com.tcmatch.tcmatch.bot.handlers.ApplicationHandler;
import com.tcmatch.tcmatch.bot.handlers.CallbackHandler;
import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class TCMatchBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final UserService userService;
    private final CallbackHandler callbackHandler;
    private final KeyboardFactory keyboardFactory;
    private final TextMessageService textMessageService;
    private final ApplicationHandler applicationHandler;

    public TCMatchBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            CallbackHandler callbackHandler,
            KeyboardFactory keyboardFactory,
            TextMessageService textMessageService, ApplicationHandler applicationHandler) {
        super(botToken); // Передаем токен в родительский класс
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userService = userService;
        this.callbackHandler = callbackHandler;
        this.keyboardFactory =  keyboardFactory;
        this.applicationHandler = applicationHandler;
        this.callbackHandler.setSender(this);
        this.textMessageService = textMessageService;
        log.info("🤖 Bot initialized: {}", botUsername);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, что это текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        String userName = message.getFrom().getUserName();
        try {
            if (text.startsWith("/start")) {
                handleStartCommand(chatId, message);
            } else {
                // 🔥 ПЕРЕДАЕМ ТЕКСТОВОЕ СООБЩЕНИЕ В ApplicationHandler
                applicationHandler.handleTextMessage(chatId, text);

                // 🔥 УДАЛЯЕМ СООБЩЕНИЕ ПОЛЬЗОВАТЕЛЯ ДЛЯ ЧИСТОТЫ ЧАТА
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId.toString());
                deleteMessage.setMessageId(message.getMessageId());
                execute(deleteMessage);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка обработки текстового сообщения: {}", e.getMessage());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        String userName = callbackQuery.getFrom().getFirstName();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("🔄 Inline button pressed: {} by {}", callbackData, userName);

        // Вся бизнес-логика в CallbackHandler
        callbackHandler.handleCallback(chatId, callbackData, userName, messageId);

        answerCallbackQuery(callbackQuery.getId());
    }

    // Вспомогательный метод для ответа на callback:
    private void answerCallbackQuery(String callbackQueryId) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .build());
        } catch (TelegramApiException e) {
            log.error("❌ Error answering callback query: {}", e.getMessage());
        }
    }

    private void handleUnknownInput(Update update) {
        Long chatId = update.getMessage().getChatId();

        String text = """
            ⚠️ Используйте кнопки для навигации
            
            Все действия выполняются через меню.
            Для начала работы нажмите /start
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.getKeyboardForUser(chatId);
        sendInlineMessage(chatId, text, keyboard);

        log.info("🚫 Unknown input from {}: {}", chatId, update.getMessage().getText());
    }

    private void handleStartCommand(Long chatId, Message message) {
        String userName = message.getFrom().getFirstName();
        String welcomeText = textMessageService.getWelcomeText(chatId, userName);

        SendMessage welcomeMessage = new SendMessage();
        welcomeMessage.setChatId(chatId.toString());
        welcomeMessage.setText(welcomeText);
        welcomeMessage.setReplyMarkup(keyboardFactory.getKeyboardForUser(chatId));

        try {
            execute(welcomeMessage);
        } catch (TelegramApiException e) {
            log.error("❌ Error sending welcome message: {}", e.getMessage());
        }
    }


    private void sendInlineMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        } else {
            log.warn("⚠️ Keyboard is null for chatId: {}, using fallback", chatId);
            // Fallback - главное меню
            message.setReplyMarkup(keyboardFactory.createMainMenuKeyboard());
        }

        try {
            execute(message);
            log.info("✅ Inline message sent to {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error sending inline message: {}", e.getMessage());
        }
    }


    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);



        try {
            execute(message);
            log.info("✅ Message sent to {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error sending message: {}", e.getMessage());
        }
    }
}