package com.tcmatch.tcmatch.bot.text;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface TextCommand {
    boolean canHandle(Long chatId, String text);
    void execute(Message message);
}
