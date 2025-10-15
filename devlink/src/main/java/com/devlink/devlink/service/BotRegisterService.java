package com.devlink.devlink.service;

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