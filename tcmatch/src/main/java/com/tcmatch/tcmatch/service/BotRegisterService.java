package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.TCMatchBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotRegisterService {

    private final TCMatchBot tcMatchBot;

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(tcMatchBot);
            log.info("✅ TCMatchBot successfully registered in Telegram!");
            log.info("🤖 Bot username: {}", tcMatchBot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("❌ Error registering bot: {}", e.getMessage());
        }
    }
}