package com.tcmatch.tcmatch.bot.handlers;

import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
import com.tcmatch.tcmatch.model.dto.BaseHandlerData;
import com.tcmatch.tcmatch.service.TextMessageService;
import com.tcmatch.tcmatch.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
public class HelpHandler extends BaseHandler {

    public HelpHandler(KeyboardFactory keyboardFactory, UserSessionService userSessionService) {
        super(keyboardFactory, userSessionService);
    }

    @Override
    public boolean canHandle(String actionType, String action) {
        return "help".equals(actionType);
    }

    @Override
    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
        BaseHandlerData data = new BaseHandlerData(chatId, messageId, userName);

        switch (action) {
            case "show_menu":
                showHelpMenu(data);
                break;
            case "rules":
                showRules(data);
                break;
            case "info":
                showInfo(data);
                break;
            case "support":
                showSupport(data);
                break;
            default:
                log.warn("❌ Unknown help action: {}", action);
        }
    }

    public void showHelpMenu(BaseHandlerData data) {
        String text = """
            ❓ **РАЗДЕЛ ПОМОЩИ**
            
            Выберите нужный раздел:
            """;

        InlineKeyboardMarkup keyboard = keyboardFactory.createHelpMenuKeyboard();
        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
    }

    public void showRules(BaseHandlerData data) {
        String offerText = TextMessageService.publicOfferText();
        editMessageWithQuote(data.getChatId(), data.getMessageId(), offerText, "🔗 **ДОГОВОР-ОФЕРТА**", offerText.length(), keyboardFactory.createBackButton());
    }

    public void showInfo(BaseHandlerData data) {
        String text = """
            ℹ️ **ИНФОРМАЦИЯ О DEVLINK**
            
            🚀 **Наша миссия:**
            Создать безопасную и удобную платформу 
            для взаимодействия заказчиков и исполнителей
            
            💡 **Основные возможности:**
            • Поиск проектов и исполнителей
            • Безопасные сделки с гарантиями
            • Система рейтингов и отзывов
            • Поэтапная оплата работ
            
            📊 **Статистика платформы:**
            • 1000+ зарегистрированных пользователей
            • 500+ успешно завершенных проектов
            • 4.8/5.0 средний рейтинг
            
            🌐 **Контакты:**
            Website: https://tcmatch.ru
            Email: info@tcmatch.ru
            """;

        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
    }

    public void showSupport(BaseHandlerData data) {
        String text = """
            🛠️ **ТЕХНИЧЕСКАЯ ПОДДЕРЖКА**
            
            Если у вас возникли проблемы или вопросы:
            
            📧 **Email поддержки:**
            support@tcmatch.ru
            
            💬 **Чат поддержки:**
            @tcmatch_support_bot
            
            ⏰ **Время работы:**
            Пн-Пт: 9:00-18:00 (МСК)
            Сб-Вс: 10:00-16:00 (МСК)
            
            🔧 **Что мы помогаем:**
            • Технические проблемы
            • Вопросы по использованию
            • Спорные ситуации
            • Предложения по улучшению
            
            📋 **Для быстрого решения проблемы:**
            Укажите в обращении:
            1. Ваш username
            2. Суть проблемы
            3. Скриншоты (если есть)
            """;

        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
    }
}
