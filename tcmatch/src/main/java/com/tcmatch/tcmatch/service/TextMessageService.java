package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TextMessageService {

    private final UserService userService;

    public String getWelcomeText(Long chatId, String userName) {
        if (!userService.userExists(chatId)) {
            return """
                    🔗 Добро пожаловать в TCMatch, %s!
                    
                    🚀 ПЛАТФОРМА ДЛЯ БЕЗОПАСНОЙ РАБОТЫ
                    Разработчиков и Заказчиков
                    
                    💡 Для начала работы нажмите:
                    "🚀 Начать регистрацию"
                    
                    🛡️ Ваша безопасность - наш приоритет!
                    """.formatted(userName);
        } else if (!userService.hasFullAccess(chatId)) {
            UserRole.RegistrationStatus status = userService.getRegistrationStatus(chatId);
            return getRegistrationProgressText(userName, status, chatId);
        } else {
            return """
                    🔗 С возвращением в TCMatch, %s!
                    
                    ✅ Регистрация завершена
                    🚀 Выберите действие из меню
                    """.formatted(userName);
        }
    }

    private String getRegistrationProgressText(String userName, UserRole.RegistrationStatus status, Long chatId) {
        UserRole userRole = userService.getUserRole(chatId);
        return switch (status) {
            case REGISTERED -> """
            🔗 С возвращением, %s!
            
            ❗ <b>Регистрация начата, но не завершена</b>
            
            📋 <b>Следующие шаги:</b>
            1. 👥 Выбрать роль (Заказчик/Исполнитель)
            2. 📖 Ознакомиться с правилами
            3. ✅ Принять правила
            
            <i>Выберите роль чтобы продолжить</i>
            """.formatted(userName);

            case ROLE_SELECTED -> """
            🔗 Рады снова видеть вас, %s!
            
            <b>Вы уже выбрали роль:<b>
            <u>%s</u>
            
            📋 <b>Следующие шаги:</b>
            1. 📖 Ознакомиться с правилами платформы
            2. ✅ Принять правила
            
            <i>Ознакомьтесь с правилами чтобы получить полный доступ</i>
            """.formatted(userName, getRoleDisplay(userRole));

            case RULES_VIEWED -> """
                🔗 Рады снова видеть вас, %s!
                
                ❗ Вы ознакомились с правилами
                
                ✅ Финальный шаг:
                Примите правила для завершения регистрации
                """.formatted(userName);
            default -> """
                🔗 Добро пожаловать, %s!
            
                ❗ <b>Статус регистрации не определен</b>
            
                💡 <b>Что делать:</b>
                • Напишите /start для перезапуска
                • Обратитесь в поддержку
            
                <i>Мы поможем решить проблему</i>
            """.formatted(userName);
        };
    }

    public String getMainMenuText() {
        return """
                🔗 <b>TCMATCH</b>
                
                🏠 <b>Главное меню</b>
                
                <i>Выберите нужный раздел:</i>
                """;
    }

    public String getRegistrationStatusMessage(UserRole.RegistrationStatus status) {
        return switch (status) {
            case REGISTERED -> "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nСледующий шаг:\nОзнакомьтесь с правилами платформы";
            case RULES_VIEWED ->  "⚠️ ВЫ УЖЕ ОЗНАКОМИЛИСЬ С ПРАВИЛАМИ\n\nФинальный шаг:\nПримите правила для завершения регистрации";
            case RULES_ACCEPTED -> "✅ Вы уже завершили регистрацию!";
            default -> "❌ Ошибка статуса";
        };
    }

    private String getRoleDisplay(UserRole role) {
        return switch (role) {
            case FREELANCER -> "👨‍💻 Исполнитель";
            case CUSTOMER -> "👔 Заказчик";
            case ADMIN -> "⚡ Администратор";
            default -> "👤 Пользователь";
        };
    }
}
