package com.tcmatch.tcmatch.bot;

import com.tcmatch.tcmatch.bot.dispatcher.CommandDispatcher;
import com.tcmatch.tcmatch.bot.dispatcher.TextCommandDispatcher;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Primary
@Slf4j
public class TCMatchBot extends TelegramLongPollingBot implements BotExecutor{

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final UserSessionService userSessionService;

    private final CommandDispatcher commandDispatcher;
    private final TextCommandDispatcher textCommandDispatcher;

    private final Map<Long, Boolean> userLocks = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastProcessingTime = new ConcurrentHashMap<>();

    private static final long CLICK_COOLDOWN_MS = 200;
    private final Map<Long, Long> lastClickTime = new ConcurrentHashMap<>();

    private final String botUsername;
    private final String botToken;
    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final TextMessageService textMessageService;
    public TCMatchBot(
            UserSessionService userSessionService,
            @Lazy CommandDispatcher commandDispatcher,
            @Lazy TextCommandDispatcher textCommandDispatcher,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            TextMessageService textMessageService,
            CommonKeyboards commonKeyboards) {
        super(botToken); // Передаем токен в родительский класс
        this.userSessionService = userSessionService;
        this.commandDispatcher = commandDispatcher;
        this.textCommandDispatcher = textCommandDispatcher;
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userService = userService;
        this.commonKeyboards =  commonKeyboards;
        this.textMessageService = textMessageService;
        log.info("🤖 Bot initialized: {}", botUsername);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(userLocks);

        Long chatId = getChatIdFromUpdate(update);

        // 🔥 ПРОВЕРКА COOLDOWN (от последнего ЗАВЕРШЕННОГО клика)
        if (isClickCooldown(chatId)) {
            return;
        }

        // 🔥 БЛОКИРОВКА НА УРОВНЕ ВХОДЯЩИХ СООБЩЕНИЙ
        if (isUserProcessing(chatId)) {
            log.warn("🚫 User {} is already processing another request - ignoring", chatId);
            return;
        }

        try {
            lockUser(chatId);
            // Проверяем, что это текстовое сообщение
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Error in onUpdateReceived for user {}: {},", chatId, e.getMessage());

        } finally {
            // 🔥 ГАРАНТИРОВАННАЯ РАЗБЛОКИРОВКА
            unlockUser(chatId);
            updateLastClickTime(chatId);
        }
    }

    private Long getChatIdFromUpdate(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    private boolean isUserProcessing(Long chatId) {
        if (chatId == null) return false;
        return userLocks.getOrDefault(chatId, false);
    }

    private void lockUser(Long chatId) {
        if (chatId != null) {
            userLocks.put(chatId, true);
            lastProcessingTime.put(chatId, System.currentTimeMillis());
            log.debug("🔒 Bot locked user: {}", chatId);
        }
    }

    private void unlockUser(Long chatId) {
        if (chatId != null) {
            userLocks.put(chatId, false);
            log.debug("🔓 Bot unlocked user: {}", chatId);
        }
    }

    // 🔥 ОЧИСТКА ВИСЯЩИХ БЛОКИРОВОК
    @Scheduled(fixedRate = 90000)
    public void cleanupStaleLocks() {
        synchronized (userLocks) {
            long now = System.currentTimeMillis();
            int removed = 0;

            for (Map.Entry<Long, Boolean> entry : userLocks.entrySet()) {
                if (entry.getValue()) { // если заблокирован
                    Long chatId = entry.getKey();
                    Long lockTime = lastProcessingTime.get(chatId);

                    if (lockTime != null && (now - lockTime) > 60000) { // 60 секунд
                        userLocks.remove(chatId);
                        removed++;
                        log.warn("🕒 Bot removed stale lock for user: {}", chatId);
                    }
                }
            }

            if (removed > 0) {
                log.info("🧹 Bot cleaned up {} stale locks", removed);
            }
        }
    }

    private boolean isClickCooldown(Long chatId) {
        if (chatId == null) return false;

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickTime.get(chatId);

        // 🔥 ПРОВЕРЯЕМ, ПРОШЛО ЛИ ДОСТАТОЧНО ВРЕМЕНИ С ПОСЛЕДНЕГО ЗАВЕРШЕННОГО КЛИКА
        if (lastTime != null && (currentTime - lastTime) < CLICK_COOLDOWN_MS) {
            log.debug("⏳ Cooldown active for user: {} ({}ms remaining)",
                    chatId, CLICK_COOLDOWN_MS - (currentTime - lastTime));
            return true;
        }

        // 🔥 НЕ ОБНОВЛЯЕМ ВРЕМЯ ЗДЕСЬ - сделаем это после выполнения логики
        return false;
    }

    private void updateLastClickTime(Long chatId) {
        if (chatId != null) {
            lastClickTime.put(chatId, System.currentTimeMillis());
            log.debug("🕒 Updated last click time for user: {}", chatId);
        }
    }


    private void handleTextMessage(Message message) {
        textCommandDispatcher.handleTextMessage(message);

//        Long chatId = message.getChatId();
//        String text = message.getText();
//        String userName = message.getFrom().getUserName();
//        Integer messageId = message.getMessageId();
//        try {
//            if (text.startsWith("/start")) {
//                handleStartCommand(chatId, message);
//                return;
//            } else {
//                textRouterService.routeTextMessage(chatId, text, messageId);
//                return;
//            }
//        } catch (Exception e) {
//            log.error("❌ Ошибка обработки текстового сообщения: {}", e.getMessage());
//        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String userName = callbackQuery.getFrom().getUserName();

        commandDispatcher.handleCallback(chatId, callbackData, messageId, userName);
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

        InlineKeyboardMarkup keyboard = commonKeyboards.getKeyboardForUser(chatId);
        sendInlineMessage(chatId, text, keyboard);

        log.info("🚫 Unknown input from {}: {}", chatId, update.getMessage().getText());
    }

    private void handleStartCommand(Long chatId, Message message) {
        String userName = message.getFrom().getFirstName();
        String welcomeText = textMessageService.getWelcomeText(chatId, userName);

        SendMessage welcomeMessage = new SendMessage();
        welcomeMessage.setChatId(chatId.toString());
        welcomeMessage.setText(welcomeText);
        welcomeMessage.setReplyMarkup(commonKeyboards.getKeyboardForUser(chatId));



        try {
            // 🔥 СОХРАНЯЕМ MESSAGE_ID ОТПРАВЛЕННОГО СООБЩЕНИЯ
            Message sentMessage = execute(welcomeMessage);
            userSessionService.setMainMessageId(chatId, sentMessage.getMessageId());

            log.info("✅ Главное сообщение сохранено для chatId {}: messageId {}", chatId, sentMessage.getMessageId());
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
            message.setReplyMarkup(commonKeyboards.createMainMenuKeyboard());
        }

        try {
            execute(message);
            log.info("✅ Inline message sent to {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error sending inline message: {}", e.getMessage());
        }
    }


    // 🔥 Реализация sendMessage (из BotExecutor)
    @Override
    public void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage(chatId.toString(), text);
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в TCMatchBot: {}", e.getMessage());
        }
    }

    // 🔥 Реализация deleteMessage (из BotExecutor)
    @Override
    public void deleteMessage(Long chatId, Integer messageId) {
        if (messageId == null) return;
        try {
            DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            // Обычно не логируем, чтобы не засорять логи при нормальном поведении (например, сообщение уже удалено)
            // log.warn("Ошибка при удалении сообщения: {}", e.getMessage());
        }
    }

    @Override
    public void editMessageWithHtml(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("HTML"); // 🔥 ВКЛЮЧАЕМ HTML-ПАРСИНГ
            editMessage.setReplyMarkup(keyboard);
            editMessage.setDisableWebPagePreview(true);

            execute(editMessage);
            log.debug("✅ HTML Message edited for: {}", chatId);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("message to edit not found")) {
                // 🔥 ПРОВЕРЯЕМ, ЭТО ГЛАВНОЕ СООБЩЕНИЕ ИЛИ ОБЫЧНОЕ?
                Integer mainMessageId = userSessionService.getMainMessageId(chatId);

                if (messageId.equals(mainMessageId) || mainMessageId == null) {
                    // 🔥 УДАЛЕНО ГЛАВНОЕ СООБЩЕНИЕ - СОЗДАЕМ НОВОЕ
                    log.warn("⚠️ Главное сообщение {} удалено, создаем новое", messageId);
                    userSessionService.setMainMessageId(chatId, null);
                    Integer newMessageId = sendHtmlMessageReturnId(chatId, text, keyboard);
                    if (newMessageId != null) {
                        userSessionService.setMainMessageId(chatId, newMessageId);
                    }
                } else {
                    // 🔥 УДАЛЕНО ОБЫЧНОЕ СООБЩЕНИЕ - ПРОСТО ЛОГИРУЕМ
                    log.warn("⚠️ Сообщение {} удалено, но это не главное сообщение", messageId);
                }
            } else {
                log.error("❌ Ошибка редактирования сообщения: {}", e.getMessage());
            }
        }
    }

    @Override
    public Integer sendHtmlMessageReturnId(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML"); // 🔥 ВКЛЮЧАЕМ HTML-ПАРСИНГ
        message.setReplyMarkup(keyboard);
        message.setDisableWebPagePreview(true);

        try {
            org.telegram.telegrambots.meta.api.objects.Message sentMessage = execute(message);
            return sentMessage.getMessageId();
        } catch (TelegramApiException e) {
            log.error("❌ Error sending HTML message: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void sendTemporaryErrorMessage(Long chatId, String errorText, int delaySeconds) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("❌ " + errorText);

            org.telegram.telegrambots.meta.api.objects.Message sentMessage = execute(message);
            Integer messageId = sentMessage.getMessageId();

            // 🔥 ПЛАНИРУЕМ УДАЛЕНИЕ ЧЕРЕЗ SCHEDULED EXECUTOR
            scheduler.schedule(() -> {
                try {
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(chatId.toString());
                    deleteMessage.setMessageId(messageId);
                    execute(deleteMessage);
                    log.debug("🗑️ Auto-deleted error message for user {}", chatId);
                } catch (Exception e) {
                    log.error("❌ Error auto-deleting message: {}", e.getMessage());
                }
            }, delaySeconds, TimeUnit.SECONDS);

        } catch (TelegramApiException e) {
            log.error("❌ Error sending temporary error message: {}", e.getMessage());
        }
    }

    @Override
    public Integer sendDocMessageReturnId(Long chatId, Resource resource, String docName) {
        try {

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setDocument(new InputFile(resource.getInputStream(), docName));

            try {
                Message sendDocMessage = execute(sendDocument);
                log.debug("✅ Docmessage sent to user {}", chatId);
                return sendDocMessage.getMessageId();
            } catch (TelegramApiException e) {
                log.error("❌Error sending docmessage to user {}", chatId);
                return null;
            }
        } catch (IOException e) {
            log.error("❌Error inputStream resource for docmessage to user {}", chatId);
            return null;
        }
    }

    @Override
    public void deleteMessages(Long chatId, List<Integer> messageIds) {
        try {
            log.info("🗑️ Deleting {} temporary messages for user {}", messageIds.size(), chatId);

            // 🔥 Выполнение команд Telegram
            for (Integer msgId : messageIds) {
                // Используем execute из библиотеки Telegram, как в вашем BaseHandler
                execute(new DeleteMessage(chatId.toString(), msgId));
            }
        } catch (TelegramApiException e) {
            log.error("❌ Error sending temporary error message: {}", e.getMessage());
        }
    }

    @Override
    public void deletePreviousMessages(Long chatId) {
        // 1. Получаем ID и очищаем сессию в одном вызове из сервиса
        List<Integer> messageIds = userSessionService.getAndClearTemporaryMessageIds(chatId);

        // 2. Если ID есть, используем BotExecutor для отправки команды
        if (!messageIds.isEmpty()) {
            // 🔥 Вызов нового метода на BotExecutor
            deleteMessages(chatId, messageIds);
        }
    }

    @Override
    public Integer getOrCreateMainMessageId(Long chatId) {
        Integer mainMessageId = userSessionService.getMainMessageId(chatId);

        // 🔥 ПРОСТО ВОЗВРАЩАЕМ ID ИЗ СЕССИИ
        // Ошибки будем обрабатывать при редактировании
        return mainMessageId != null ? mainMessageId : createNewMainMessage(chatId);
    }

    private boolean isMessageExists(Long chatId, Integer messageId) {
        try {
            // Пытаемся отредактировать сообщение (просто чтобы проверить его существование)
            EditMessageText testEdit = new EditMessageText();
            testEdit.setChatId(chatId.toString());
            testEdit.setMessageId(messageId);
            testEdit.setText("⏳ Подождите");
            execute(testEdit);
            return true;
        } catch (TelegramApiException e) {
            return false;
        }
    }

    private Integer createNewMainMessage(Long chatId) {
        try {
            // Удаляем все временные сообщения
            deletePreviousMessages(chatId);

            // Создаем новое главное сообщение
            String text = textMessageService.getMainMenuText();
            InlineKeyboardMarkup keyboard = commonKeyboards.createMainMenuKeyboard();

            Integer newMainMessageId = sendHtmlMessageReturnId(chatId, text, keyboard);
            if (newMainMessageId != null) {
                userSessionService.setMainMessageId(chatId, newMainMessageId);
                log.info("🔄 Created new main message for user {}: messageId {}", chatId, newMainMessageId);
                return newMainMessageId;
            }
        } catch (Exception e) {
            log.error("❌ Error creating new main message for user {}: {}", chatId, e.getMessage());
        }
        return null;
    }

    @Override
    public void sendTemporaryErrorMessageWithHtml(Long chatId, String errorText, int delaySeconds) {
        try {
            SendMessage message = new SendMessage();
            message.setParseMode("HTML");
            message.setChatId(chatId.toString());
            message.setText("❌ " + errorText);

            org.telegram.telegrambots.meta.api.objects.Message sentMessage = execute(message);
            Integer messageId = sentMessage.getMessageId();

            // 🔥 ПЛАНИРУЕМ УДАЛЕНИЕ ЧЕРЕЗ SCHEDULED EXECUTOR
            scheduler.schedule(() -> {
                try {
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(chatId.toString());
                    deleteMessage.setMessageId(messageId);
                    execute(deleteMessage);
                    log.debug("🗑️ Auto-deleted error message for user {}", chatId);
                } catch (Exception e) {
                    log.error("❌ Error auto-deleting message: {}", e.getMessage());
                }
            }, delaySeconds, TimeUnit.SECONDS);

        } catch (TelegramApiException e) {
            log.error("❌ Error sending temporary error message: {}", e.getMessage());
        }
    }
}