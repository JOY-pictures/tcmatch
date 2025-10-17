package com.devlink.devlink.bot;

import com.devlink.devlink.model.RegistrationStatus;
import com.devlink.devlink.model.User;
import com.devlink.devlink.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class DevLinkBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final UserService userService;

    public DevLinkBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            UserService userService) {
        super(botToken); // Передаем токен в родительский класс
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userService = userService;
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
        String userName = update.getMessage().getFrom().getUserName();
        String userLastName = update.getMessage().getFrom().getLastName();


        log.info("📨 Message from {} ({}): {}", userFirstName, chatId, messageText);

        // Обрабатываем команды
        switch (messageText) {
            case "/start":
                handleStartCommand(chatId, userFirstName);
                break;

            case "/register":
                handleRegisterCommand(chatId, userFirstName, userName, userLastName);
                break;

            case "/rules":
                handleRulesCommand(chatId);
                break;

            case "/accept_rules":
                handleAcceptRulesCommand(chatId, userFirstName);
                break;

//            case "/profile":
//                handleProfileCommand(chatId);
//                break;

            case "/help":
                handleHelpCommand(chatId);
                break;

            default:
                handleUnknownCommand(chatId);
        }
    }

    private void handleStartCommand(Long chatId, String userName) {
        String welcomeText = """
            🔗 Добро пожаловать в DevLink, %s!
            
            🚀 ПЛАТФОРМА ДЛЯ БЕЗОПАСНОЙ РАБОТЫ
            Разработчиков и Заказчиков
            
            💡 ОСНОВНЫЕ ВОЗМОЖНОСТИ:
            • Безопасные сделки с Escrow-системой
            • Гарантия оплаты для исполнителей
            • Гарантия качества для заказчиков  
            • Мгновенные выплаты после принятия работы
            • Минимальная комиссия
            
            📋 ДЛЯ НАЧАЛА РАБОТЫ:
            1. Ознакомьтесь с правилами: /rules
            2. Зарегистрируйтесь: /register
            3. Начните работу: /help
            
            🛡️ Ваша безопасность - наш приоритет!
            """.formatted(userName);

        sendMessage(chatId, welcomeText);
        log.info("✅ Sent welcome message to {}", chatId);
    }

    private void handleRegisterCommand(Long chatId, String firstName, String username, String lastName) {
        // ПРОВЕРЯЕМ, существует ли уже пользователь
        if (userService.userExists(chatId)) {
            RegistrationStatus currentStatus = userService.getRegistrationStatus(chatId);

            String message = switch (currentStatus) {
                case REGISTERED, RULES_VIEWED -> """
                        ⚠️ ВЫ УЖЕ ЗАРЕГИСТРИРОВАНЫ
                        
                        Следующий шаг:
                        /rules - ознакомиться с правилами
                        """;
                case RULES_ACCEPTED -> "✅ Вы уже завершили регистрацию!";
                default -> "❌ Ошибка статуса. Используйте /start";
            };

            sendMessage(chatId, message);
            return;
        }

        // ЕСЛИ пользователя НЕТ - регистрируем
        User user = userService.registerFromTelegram(chatId, username, firstName, lastName);

        sendMessage(chatId, """
                ✅ АККАУНТ СОЗДАН!
                
                Добро пожаловать, %s!
                
                Для завершения регистрации необходимо:
                
                📜 ОЗНАКОМИТЬСЯ С ПРАВИЛАМИ
                Обязательно прочтите правила использования платформы:
                /rules - полный текст правил
                
                ⚠️ ВАЖНО:
                • Без принятия правил функционал платформы будет ограничен
                • Все сделки защищены Escrow-системой
                • Мы гарантируем безопасность ваших средств
                
                🔐 Ваши данные защищены и не передаются третьим лицам.
                """.formatted(firstName));
    }

    private void handleRulesCommand(Long chatId) {

        if (!userService.userExists(chatId)) {
            sendMessage(chatId, """
            ❌ СНАЧАЛА ЗАРЕГИСТРИРУЙТЕСЬ
            /register - создание аккаунта
            """);
            return;
        }

        String rulesText;

        RegistrationStatus currentStatus = userService.getRegistrationStatus(chatId);

        switch (currentStatus) {
            case NOT_REGISTERED:
                // Теоретически не должно случиться, но на всякий случай
                sendMessage(chatId, "❌ Ошибка статуса. Используйте /register");
                break;

            case RULES_ACCEPTED :
                rulesText = """
                    📜 ПРАВИЛА ИСПОЛЬЗОВАНИЯ DEVLINK
                    
                    ✅ ОБЯЗАТЕЛЬНЫЕ ПРАВИЛА:
                    
                    1. 🛡️ Безопасность сделок
                    • Все платежи через защищенный Escrow-счет
                    • Деньги блокируются до подтверждения работы
                    • Исполнитель получает оплату после одобрения
                    
                    2. 💰 Прозрачность оплаты
                    • Точный бюджет при создании проекта
                    • Все дополнительные работы через систему правок
                    • Без скрытых комиссий
                    
                    3. ⏱️ Соблюдение сроков
                    • Исполнитель: уложиться в дедлайн
                    • Заказчик: проверить работу за 48 часов
                    • Авто-подтверждение через 2 дня
                    
                    4. 💬 Профессиональное общение
                    • Общение через встроенный чат DevLink
                    • Запрещены оскорбления
                    • Конфликты через модерацию
                    
                    5. 🚫 ЗАПРЕЩЕНО:
                    • Прямые переводы минуя Escrow
                    • Обмен контактами до сделки
                    • Мошенничество и обман
                    
                    ⚠️ НАРУШЕНИЕ ПРАВИЛ:
                    • 1-е нарушение - предупреждение
                    • 2-е нарушение - блокировка 7 дней
                    • 3-е нарушение - постоянный бан
                    
                    💡 Вы уже приняли правила ранее. Это повторный просмотр.
                    
                    ❓ ПОМОЩЬ:
                    /help - все команды
                    """;
                sendMessage(chatId, rulesText);


                break;
            case REGISTERED, RULES_VIEWED:
                rulesText = """
                    📜 ПРАВИЛА DEVLINK
                    
                    1. 🛡️ Безопасность сделок
                    • Все платежи через защищенный Escrow-счет
                    • Деньги блокируются до подтверждения работы
                    • Исполнитель получает оплату после одобрения
                    
                    2. 💰 Прозрачность оплаты
                    • Точный бюджет при создании проекта
                    • Все дополнительные работы через систему правок
                    • Без скрытых комиссий
                    
                    3. ⏱️ Соблюдение сроков
                    • Исполнитель: уложиться в дедлайн
                    • Заказчик: проверить работу за 48 часов
                    • Авто-подтверждение через 2 дня
                    
                    4. 💬 Профессиональное общение
                    • Общение через встроенный чат DevLink
                    • Запрещены оскорбления
                    • Конфликты через модерацию
                    
                    5. 🚫 ЗАПРЕЩЕНО:
                    • Прямые переводы минуя Escrow
                    • Обмен контактами до сделки
                    • Мошенничество и обман
                    
                    ⚠️ НАРУШЕНИЕ ПРАВИЛ:
                    • 1-е нарушение - предупреждение
                    • 2-е нарушение - блокировка 7 дней
                    • 3-е нарушение - постоянный бан
                    
                    ✅ ДЛЯ ПОДТВЕРЖДЕНИЯ:
                    /accept_rules - я ознакомился и принимаю правила
                    
                    ❓ ВОПРОСЫ:
                    /help - помощь
                    """;

                sendMessage(chatId, rulesText);
                userService.markRulesViewed(chatId);
                break;
            }

    }

    private void handleAcceptRulesCommand(Long chatId, String firstName) {
        // ПРОВЕРЯЕМ, зарегистрирован ли пользователь
        if (!userService.userExists(chatId)) {
            sendMessage(chatId, """
            ❌ СНАЧАЛА ЗАРЕГИСТРИРУЙТЕСЬ
            
            Для принятия правил необходимо создать аккаунт:
            /register - регистрация в платформе
            """);
            return;
        }

        // ПОЛУЧАЕМ текущий статус пользователя
        RegistrationStatus currentStatus = userService.getRegistrationStatus(chatId);

        switch (currentStatus) {
            case NOT_REGISTERED:
                // Теоретически не должно случиться, но на всякий случай
                sendMessage(chatId, "❌ Ошибка статуса. Используйте /register");
                break;

            case REGISTERED:
                sendMessage(chatId, """
                ❌ СНАЧАЛА ОЗНАКОМЬТЕСЬ С ПРАВИЛАМИ
                
                Прежде чем принимать правила, необходимо их прочитать:
                /rules - полный текст правил платформы
                """);
                break;

            case RULES_VIEWED:
                // ВСЕ ПРАВИЛА СОБЛЮДЕНЫ - ПРИНИМАЕМ ПРАВИЛА
                User user = userService.acceptRules(chatId);

                String successText = """
                🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!
                
                %s, добро пожаловать в DevLink!
                
                ✅ Статус: %s
                📅 Принято: %s
                
                🚀 ТЕПЕРЬ ВАМ ДОСТУПНО:
                • Создание проектов (/new_project)
                • Поиск работы (/browse)
                • Просмотр профиля (/profile)
                • Безопасные сделки с Escrow
                
                💡 НАЧНИТЕ РАБОТУ:
                /help - все команды платформы
                """.formatted(
                        firstName,
                        user.getRegistrationStatus(),
                        user.getRulesAcceptedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                );

                sendMessage(chatId, successText);
                log.info("🎉 User completed registration: {}", chatId);
                break;

            case RULES_ACCEPTED:
                sendMessage(chatId, """
                ✅ ВЫ УЖЕ ПРИНЯЛИ ПРАВИЛА
                
                Регистрация уже завершена. Можете начинать работу!
                
                💡 КОМАНДЫ ДЛЯ РАБОТЫ:
                /help - все команды
                /profile - ваш профиль
                """);
                break;
        }
    }
    
   private void handleProfileCommand(Long chatId) {
    // ПРОВЕРЯЕМ, завершил ли пользователь регистрацию
    if (!userService.hasFullAccess(chatId)) {
        RegistrationStatus status = userService.getRegistrationStatus(chatId);
        
        String message = switch (status) {
            case NOT_REGISTERED -> """
                ❌ ДОСТУП ЗАКРЫТ
                
                Для доступа к профилю необходимо:
                /register - начать регистрацию
                """;
                
            case REGISTERED -> """
                ❌ ДОСТУП ЗАКРЫТ
                
                Следующий шаг:
                /rules - ознакомиться с правилами
                """;
                
            case RULES_VIEWED -> """
                ❌ ДОСТУП ЗАКРУТ
                
                Финальный шаг:
                /accept_rules - принять правила
                """;
                
            default -> "❌ Ошибка статуса. Используйте /start";
        };
        
        sendMessage(chatId, message);
        return;
    }
    
    // ЕСЛИ регистрация завершена - показываем профиль
    User user = userService.findByChatId(chatId).orElseThrow();
    
    // Форматируем даты
    String registeredDate = user.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    String rulesAcceptedDate = user.getRulesAcceptedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    
    String profileText = """
        👤 ВАШ ПРОФИЛЬ DEVLINK
        
        📝 Имя: %s %s
        🔗 Username: @%s
        💼 Роль: %s
        ⭐ Рейтинг: %.1f/5.0
        📅 В системе с: %s
        ✅ Правила приняты: %s
        
        💡 Статистика:
        • Завершенных сделок: 0
        • Открытых проектов: 0
        • Активных откликов: 0
        
        🛠️ Доступные действия:
        /browse - найти проекты (скоро)
        /new_project - создать проект (скоро)
        /help - все команды
        """.formatted(
            user.getFirstName(),
            user.getLastName() != null ? user.getLastName() : "",
            user.getUsername() != null ? user.getUsername() : "не указан",
            user.getRole(),
            user.getRating(),
            registeredDate,
            rulesAcceptedDate
        );
    
    sendMessage(chatId, profileText);
    log.info("📊 Profile shown for user: {}", chatId);
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
