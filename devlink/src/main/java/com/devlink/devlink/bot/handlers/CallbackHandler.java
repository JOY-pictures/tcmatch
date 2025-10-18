package com.devlink.devlink.bot.handlers;


import com.devlink.devlink.bot.keyboards.KeyboardFactory;
import com.devlink.devlink.model.RegistrationStatus;
import com.devlink.devlink.model.User;
import com.devlink.devlink.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackHandler {


    private final UserService userService;
    private final KeyboardFactory keyboardFactory;
    private AbsSender sender;

    public void setSender(AbsSender sender) {
        this.sender = sender;
    }

    public void handleCallback(Long chatId, String callbackData, String userName, Integer messageId) {
        String[] parts = callbackData.split(":");
        String actionType = parts[0];
        String action = parts[1];
        String parameter = parts.length > 2 ? parts[2] : null;

        switch (actionType) {
            case "menu":
                handleMenuAction(chatId, action, messageId);
                break;
            case "register":
                handleRegistrationAction(chatId, action, userName, messageId);
                break;
            case "rules":
                handleRulesAction(chatId, action, userName, messageId);
                break;
            case "projects":
                handleProjectAction(chatId, action, parameter, messageId);
                break;
        }
    }

    public void handleMenuAction(Long chatId, String action, Integer messageId) {
        switch (action) {
            case "profile":
                showUserProfile(chatId, messageId);
                break;
            case "projects":
                showProjectsList(chatId, messageId);
                break;
            case "create_project":
                showCreateProjectForm(chatId, messageId);
                break;
            case "browse_freelancers":
                showFreelancersList(chatId, messageId);
                break;
            case "my_orders":
                showMyOrders(chatId, messageId);
                break;
            case "help":
                showHelp(chatId, messageId);
                break;
            case "about":
                showAboutInfo(chatId, messageId);
                break;
            default:
                log.warn("❌ Unknown menu action: {}", action);
        }
    }

    public String getWelcomeText(Long chatId, String userName) {
        if (!userService.userExists(chatId)) {
            return """
                    🔗 Добро пожаловать в DevLink, %s!
                    
                    🚀 ПЛАТФОРМА ДЛЯ БЕЗОПАСНОЙ РАБОТЫ
                    Разработчиков и Заказчиков
                    
                    💡 Для начала работы нажмите:
                    "🚀 Начать регистрацию"
                    
                    🛡️ Ваша безопасность - наш приоритет!
                    """.formatted(userName);
        } else if (!userService.hasFullAccess(chatId)) {
            RegistrationStatus status = userService.getRegistrationStatus(chatId);
            return getRegistrationProgressText(userName, status);
        } else {
            return """
                    🔗 С возвращением в DevLink, %s!
                    
                    ✅ Регистрация завершена
                    🚀 Выберите действие из меню
                    """.formatted(userName);
        }
    }

    private String getRegistrationProgressText(String userName, RegistrationStatus status) {
        return switch (status) {
            case REGISTERED -> """
                🔗 С возвращением, %s!
                
                ❗ Вы зарегистрированы, но ещё не ознакомились с правилами
                
                📋 Следующий шаг:
                Ознакомьтесь с правилами платформы
                """.formatted(userName);

            case RULES_VIEWED -> """
                🔗 Рады снова видеть вас, %s!
                
                ❗ Вы ознакомились с правилами
                
                ✅ Финальный шаг:
                Примите правила для завершения регистрации
                """.formatted(userName);

            default -> """
                🔗 Добро пожаловать, %s!
                
                ❗ Ваша регистрация не завершена
                """.formatted(userName);
        };
    }

    public void handleRegistrationAction(Long chatId, String action, String userName, Integer messageId) {
        switch (action) {
            case "start":
                startRegistration(chatId, userName, messageId);
                break;
            default:
                log.warn("❌ Unknown register action: {}", action);
        }
    }

    public void handleRulesAction(Long chatId, String action, String userName, Integer messageId) {
        switch (action) {
            case "view":
                showFullRules(chatId, messageId);
                break;
            case "accept":
                acceptRules(chatId, userName, messageId);
                break;
            case "preview":
                showRulesPreview(chatId, messageId);
                break;
            default:
                log.warn("❌ Unknown rules action: {}", action);
        }
    }

    public void handleProjectAction(Long chatId, String action, String parameter, Integer messageId) {
        String text = "🚧 Раздел проектов в разработке...";
        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, text, keyboard);
    }

    public void startRegistration(Long chatId, String userName, Integer messageId) {
        if (userService.userExists(chatId)) {
            // Показываем текущий статус регистрации
            RegistrationStatus status = userService.getRegistrationStatus(chatId);
            String message = getRegistrationStatusMessage(status);
            InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(status);
            editMessage(chatId, messageId, message, keyboard);
            return;
        }

        User user = userService.registerFromTelegram(chatId, userName, null, null);
        String text = """
            🚀 РЕГИСТРАЦИЯ НАЧАТА!
            
            Добро пожаловать, %s!
            
            📋 Следующий шаг:
            Ознакомьтесь с правилами платформы
            """.formatted(userName);

        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(RegistrationStatus.REGISTERED);
        editMessage(chatId, messageId, text, keyboard);
        log.info("🚀 Registration started via callback for: {}", chatId);
    }

    private void showFullRules(Long chatId, Integer messageId) {
        userService.markRulesViewed(chatId);

        String rulesText = """
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
                
                ✅ ДЛЯ ПОДТВЕРЖДЕНИЯ:
                Нажмите "✅ Принять правила"
                """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(RegistrationStatus.RULES_VIEWED);
        editMessage(chatId, messageId, rulesText, keyboard);
    }

    private void acceptRules(Long chatId, String userName, Integer messageId) {
        User user = userService.acceptRules(chatId);

        String successText = """
            🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!
            
            %s, добро пожаловать в DevLink!
            
            ✅ Статус: %s
            📅 Принято: %s
            
            🚀 Теперь вам доступен полный функционал платформы
            """.formatted(
                userName,
                user.getRegistrationStatus(),
                user.getRulesAcceptedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, successText, keyboard);
        log.info("🎉 User completed registration via callback: {}", chatId);
    }

    private void showUserProfile(Long chatId, Integer messageId) {
        if (!userService.hasFullAccess(chatId)) {
            String message = "❌ Для доступа к профилю завершите регистрацию";
            InlineKeyboardMarkup keyboard = keyboardFactory.getKeyboardForUser(chatId);
            editMessage(chatId, messageId, message, keyboard);
            return;
        }

        User user = userService.findByChatId(chatId).orElseThrow();

        String profileText = """
            👤 ВАШ ПРОФИЛЬ DEVLINK
            
            📝 Имя: %s %s
            🔗 Username: @%s
            💼 Роль: %s
            ⭐ Рейтинг: %.1f/5.0
            📅 В системе с: %s
            
            💡 Статистика:
            • Завершенных сделок: 0
            • Открытых проектов: 0
            • Активных откликов: 0
            """.formatted(
                user.getFirstname(),
                user.getLastname() != null ? user.getLastname() : "",
                user.getUsername() != null ? user.getUsername() : "не указан",
                user.getRole(),
                user.getRating(),
                user.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        );

        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, profileText, keyboard);
    }

    private String getRegistrationStatusMessage(RegistrationStatus status) {
        return switch (status) {
            case REGISTERED -> "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nСледующий шаг:\nОзнакомьтесь с правилами платформы";
            case RULES_VIEWED ->  "⚠️ ВЫ УЖЕ ОЗНАКОМИЛИСЬ С ПРАВИЛАМИ\n\nФинальный шаг:\nПримите правила для завершения регистрации";
            case RULES_ACCEPTED -> "✅ Вы уже завершили регистрацию!";
            default -> "❌ Ошибка статуса";
        };
    }

    private void showRulesPreview(Long chatId, Integer messageId) {
        String previewText = """
            📋 ОСНОВНЫЕ ПРАВИЛА DEVLINK
            
            🛡️ Безопасность:
            • Все платежи через Escrow-систему
            • Гарантия оплаты для исполнителей
            • Гарантия качества для заказчиков
            
            💰 Прозрачность:
            • Фиксированные бюджеты проектов
            • Без скрытых комиссий
            • Мгновенные выплаты
            
            ⚠️ Полные правила будут доступны после регистрации
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createUnauthorizedUserKeyboard();
        editMessage(chatId, messageId, previewText, keyboard);
    }

    private void showAboutInfo(Long chatId, Integer messageId) {
        String aboutText = """
            ℹ️ О ПРОЕКТЕ DEVLINK
            
            🚀 Платформа для безопасной работы
            разработчиков и заказчиков
            
            💡 Наша миссия:
            Создать экосистему, где каждая сторона
            защищена и уверена в результате
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createUnauthorizedUserKeyboard();
        editMessage(chatId, messageId, aboutText, keyboard);
    }

    private void showProjectsList(Long chatId, Integer messageId) {
        String text = "🚧 Раздел проектов в разработке...";
        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, text, keyboard);
    }

    private void showCreateProjectForm(Long chatId, Integer messageId) {
        String text = "🚧 Создание проектов в разработке...";
        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, text, keyboard);
    }

    private void showFreelancersList(Long chatId, Integer messageId) {
        String text = "🚧 Поиск исполнителей в разработке...";
        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, text, keyboard);
    }

    private void showMyOrders(Long chatId, Integer messageId) {
        String text = "🚧 Раздел заказов в разработке...";
        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
        editMessage(chatId, messageId, text, keyboard);
    }

    private void showHelp(Long chatId, Integer messageId) {
        String helpText = """
            🆘 Помощь по DevLink
            
            💡 Основные возможности:
            • Безопасные сделки с Escrow
            • Поиск проектов и исполнителей
            • Система рейтингов и отзывов
            
            🚀 Скоро появится:
            • Создание проектов
            • Система платежей
            • Чат между участниками
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.getKeyboardForUser(chatId);
        editMessage(chatId, messageId, helpText, keyboard);
    }

    private void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        editMessage.setReplyMarkup(keyboard);

        try {
            sender.execute(editMessage);
            log.info("✅ Message edited for: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("❌ Error editing message: {}", e.getMessage());
        }
    }
}
