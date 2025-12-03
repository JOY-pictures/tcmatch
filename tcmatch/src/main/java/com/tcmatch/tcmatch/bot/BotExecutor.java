package com.tcmatch.tcmatch.bot;

import org.springframework.core.io.Resource;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;

public interface BotExecutor {
    <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException;


    /**
     * Хелпер для простой отправки текстового сообщения.
     */
    void sendMessage(Long chatId, String text);

    /**
     * Хелпер для простого удаления сообщения.
     */
    void deleteMessage(Long chatId, Integer messageId);

    void editMessageWithHtml(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard);

    Integer sendHtmlMessageReturnId(Long chatId, String text, InlineKeyboardMarkup keyboard);

    void sendTemporaryErrorMessage(Long chatId, String errorText, int delaySeconds);

    Integer sendDocMessageReturnId(Long chatId, Resource resource, String docName);

    void deleteMessages(Long chatId, List<Integer> messageIds); // 🔥 Новый метод

    void deletePreviousMessages(Long chatId);

    Integer getOrCreateMainMessageId(Long chatId);

    void sendTemporaryErrorMessageWithHtml(Long chatId, String errorText, int delaySeconds);
}