package com.devlink.devlink.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@Slf4j
public class DevLinkBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;

    public DevLinkBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken) {
        super(botToken); // Передаем токен в родительский класс
        this.botUsername = botUsername;
        this.botToken = botToken;
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
            handleTextMessage(update);
        }
    }

    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String userFirstName = update.getMessage().getFrom().getFirstName();

        log.info("📨 Message from {} ({}): {}", userFirstName, chatId, messageText);

        // Обрабатываем команды
        switch (messageText) {
            case "/start":
                handleStartCommand(chatId, userFirstName);
                break;
            case "/help":
                handleHelpCommand(chatId);
                break;
            default:
                handleUnknownCommand(chatId);
        }
    }

    private void handleStartCommand(Long chatId, String userName) {
        String welcomeText = """
            🔗 Привет, %s! Добро пожаловать в DevLink!
            
            Это платформа для безопасной работы разработчиков и заказчиков.
            
            🚀 Возможности:
            • Безопасные сделки с Escrow
            • Мгновенные выплаты  
            • Минимальная комиссия
            
            Используйте команды:
            /help - помощь
            """.formatted(userName);

        sendMessage(chatId, welcomeText);
        log.info("✅ Sent welcome message to {}", chatId);
    }

    private void handleHelpCommand(Long chatId) {
        String helpText = """
            🆘 Помощь по DevLink
            
            Основные команды:
            /start - регистрация и начало работы
            /help - эта справка
            
            💡 Скоро появится:
            • Создание проектов
            • Поиск работы
            • Безопасные сделки
            """;

        sendMessage(chatId, helpText);
    }

    private void handleUnknownCommand(Long chatId) {
        String text = "❌ Неизвестная команда. Используйте /help для списка команд.";
        sendMessage(chatId, text);
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
