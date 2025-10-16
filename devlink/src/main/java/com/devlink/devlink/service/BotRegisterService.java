package com.devlink.devlink.service;

import com.devlink.devlink.bot.DevLinkBot;
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

    private final DevLinkBot devLinkBot;

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(devLinkBot);
            log.info("✅ DevLinkBot successfully registered in Telegram!");
            log.info("🤖 Bot username: {}", devLinkBot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("❌ Error registering bot: {}", e.getMessage());
        }
    }
}