package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseHandler {
    protected final KeyboardFactory keyboardFactory;
    protected final UserSessionService userSessionService;
    protected AbsSender sender;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void setSender(AbsSender sender) {
        this.sender = sender;
    }

    // 🔥 ЗАМЕНЯЕМ СТАРЫЕ МЕТОДЫ НА НОВЫЕ

    protected void saveMainMessageId(Long chatId, Integer messageId) {
        userSessionService.setMainMessageId(chatId, messageId);
    }

    protected Integer getMainMessageId(Long chatId) {
        return userSessionService.getMainMessageId(chatId);
    }

    protected void deletePreviousProjectMessages(Long chatId) {
        List<Integer> messageIds = userSessionService.getTemporaryMessageIds(chatId);

        if (!messageIds.isEmpty()) {
            log.info("🗑️ Deleting {} temporary messages for user {}", messageIds.size(), chatId);
            for (Integer msgId : messageIds) {
                deleteMessage(chatId, msgId);
            }
        }

        userSessionService.clearTemporaryMessages(chatId);
    }

    protected void saveProjectMessageIds(Long chatId, List<Integer> messageIds) {
        userSessionService.clearTemporaryMessages(chatId);
        for (Integer messageId : messageIds) {
            userSessionService.addTemporaryMessageId(chatId, messageId);
        }
        log.info("💾 Saved {} temporary message IDs for user: {}", messageIds.size(), chatId);
    }

    protected void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        editMessage.setReplyMarkup(keyboard);

        try {
            sender.execute(editMessage);
            log.debug("✅ Message edited for: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error editing message: {}", e.getMessage());
        }
    }

    protected  void showMainMenu(Long chatId, Integer messageId) {
        String text = "🔗**TCMATCH **\n\n🏠Главное меню";
        editMessage(chatId, messageId, text, keyboardFactory.createMainMenuKeyboard());
    }

    //Отправить сообщение с ошибкой
    protected void sendErrorMessage(Long chatId, String errorText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("❌ " + errorText);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Error sending error message: {}", e.getMessage());
        }
    }

    protected void editMessageWithQuote(Long chatId, Integer messageId, String text, String startQuote, Integer quoteLength, InlineKeyboardMarkup keyboard) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setText(text);
        editMessage.setMessageId(messageId);
        editMessage.setReplyMarkup(keyboard);

        List<MessageEntity> entities = new ArrayList<>();
        MessageEntity publicOfferQuoteEntity = new MessageEntity();
        publicOfferQuoteEntity.setType("blockquote");
        publicOfferQuoteEntity.setOffset(text.indexOf(startQuote));
        publicOfferQuoteEntity.setLength(quoteLength);
        entities.add(publicOfferQuoteEntity);

        editMessage.setEntities(entities);

        try {
            sender.execute(editMessage);
            log.debug("✅ Message edited for: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error editing message: {}", e.getMessage());
        }

    }

    protected  Integer sendInlineMessageReturnId(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            org.telegram.telegrambots.meta.api.objects.Message sentMessage = sender.execute(message);
            return sentMessage.getMessageId();
        } catch (TelegramApiException e) {
            log.error("❌ Error sending project message: {}", e.getMessage());
            return null;
        }
    }

    protected void deleteMessage(Long chatId, Integer messageId) {
        if (messageId == null) return;

        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);

        try {
            sender.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("❌ Error deleting message: {}", e.getMessage());
        }
    }

    protected  Integer sendInlineMessageWithQuoteReturnId(Long chatId, String text, String startQuote, Integer quoteLength, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(keyboard);

        List<MessageEntity> entities = new ArrayList<>();
        MessageEntity publicOfferQuoteEntity = new MessageEntity();
        publicOfferQuoteEntity.setType("blockquote");
        publicOfferQuoteEntity.setOffset(text.indexOf(startQuote));
        publicOfferQuoteEntity.setLength(quoteLength);
        entities.add(publicOfferQuoteEntity);

        sendMessage.setEntities(entities);

        try {
            org.telegram.telegrambots.meta.api.objects.Message sentMessage = sender.execute(sendMessage);
            return sentMessage.getMessageId();
        } catch (TelegramApiException e) {
            log.error("❌ Error sending project message: {}", e.getMessage());
            return null;
        }
    }

    protected void editMessageWithHtml(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("HTML"); // 🔥 ВКЛЮЧАЕМ HTML-ПАРСИНГ
            editMessage.setReplyMarkup(keyboard);
            editMessage.setDisableWebPagePreview(true);

            sender.execute(editMessage);
            log.debug("✅ HTML Message edited for: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error editing HTML message: {}", e.getMessage());
        }
    }

    protected Integer sendHtmlMessageReturnId(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML"); // 🔥 ВКЛЮЧАЕМ HTML-ПАРСИНГ
        message.setReplyMarkup(keyboard);
        message.setDisableWebPagePreview(true);

        try {
            org.telegram.telegrambots.meta.api.objects.Message sentMessage = sender.execute(message);
            return sentMessage.getMessageId();
        } catch (TelegramApiException e) {
            log.error("❌ Error sending HTML message: {}", e.getMessage());
            return null;
        }
    }

    protected void sendTemporaryErrorMessage(Long chatId, String errorText, int delaySeconds) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("❌ " + errorText);

            org.telegram.telegrambots.meta.api.objects.Message sentMessage = sender.execute(message);
            Integer messageId = sentMessage.getMessageId();

            // 🔥 ПЛАНИРУЕМ УДАЛЕНИЕ ЧЕРЕЗ SCHEDULED EXECUTOR
            scheduler.schedule(() -> {
                try {
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(chatId.toString());
                    deleteMessage.setMessageId(messageId);
                    sender.execute(deleteMessage);
                    log.debug("🗑️ Auto-deleted error message for user {}", chatId);
                } catch (Exception e) {
                    log.error("❌ Error auto-deleting message: {}", e.getMessage());
                }
            }, delaySeconds, TimeUnit.SECONDS);

        } catch (TelegramApiException e) {
            log.error("❌ Error sending temporary error message: {}", e.getMessage());
        }
    }


    public abstract boolean canHandle(String actionType, String action);
    public abstract void handle(Long chatId, String action, String parameter, Integer messageId, String userName);
}
