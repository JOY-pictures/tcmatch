package com.tcmatch.tcmatch.bot.handlers;


import com.tcmatch.tcmatch.service.NavigationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CallbackHandler {

    private final NavigationService navigationService;
    private final List<BaseHandler> handlers;
    private final Map<Long, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long CLICK_COOLDOWN_MS = 500;

    public CallbackHandler(List<BaseHandler> handlers, NavigationService navigationService) {
        this.handlers = handlers;
        this.navigationService = navigationService;
    }

    public void setSender(AbsSender sender) {
        handlers.forEach(handler -> handler.setSender(sender));
    }

    public void handleCallback(Long chatId, String callbackData, String userName, Integer messageId) {

        // Защита от спама
        if (isClickCooldown(chatId)) return;

        log.info("🔄 Handling callback: {} from user {}", callbackData, chatId);

        String[] parts = callbackData.split(":", 3);
        String actionType = parts[0];
        String action = parts[1];
        String parameter = parts.length > 2 ? parts[2] : null;

        saveToNavigationHistory(chatId, actionType, action, parameter);

        for (BaseHandler handler : handlers) {
            if (handler.canHandle(actionType, action)) {
                handler.handle(chatId, action, parameter, messageId, userName);
                return;
            }
        }

        // Если не нашли обработчик
        log.warn("⚠️ No handler found for: {}:{}", actionType, action);
    }

    private boolean isClickCooldown(Long chatId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastClickTime.get(chatId);

        if (lastTime != null && (currentTime - lastTime) < CLICK_COOLDOWN_MS) {
            log.debug("⏳ Click cooldown for user: {}", chatId);
            return true; // Игнорируем быстрое повторное нажатие
        }

        lastClickTime.put(chatId, currentTime);
        return false;

    }

    //Сохраняет действие в историю навигации (если нужно)
    private void saveToNavigationHistory(Long chatId, String actionType, String action, String parameter) {
        // НЕ сохраняем в историю:
        // - навигацию "назад"
        // - фильтры проектов
        // - пагинацию
        boolean shouldNotSave = "navigation".equals(actionType) && "back".equals(action) ||
                "project".equals(actionType) && ("filter".equals(action) || "page".equals(action)) ||
//                "user_profile".equals(actionType) && "edit".equals(action) || // редактирование профиля
                "projects".equals(actionType) //&& ("search".equals(action) || "filter".equals(action)); // подразделы проектов
//                "freelancers".equals(actionType) && ("search".equals(action) || "favorites".equals(action)) || // подразделы исполнителей
//                "help".equals(actionType) && ("rules".equals(action) || "info".equals(action) || "support".equals(action) // подразделы помощи
                ;

        if (shouldNotSave) {
            log.debug("📱 Skipping history save for: {}:{}", actionType, action);
            return;
        }

        // Формируем идентификатор экрана
        String screen = actionType + ":" + action + (parameter != null ? ":" + parameter : "");

        // Проверяем, не является ли этот экран уже текущим
        String currentScreen = navigationService.getCurrentScreen(chatId);
        if (screen.equals(currentScreen)) {
            log.debug("📱 Screen already current, skipping: {}", screen);
            return;
        }

        // Сохраняем в историю
        navigationService.pushScreen(chatId, screen);
        log.debug("📱 Added to navigation history: {}", screen);
    }
//
//    private void saveProjectMessageIds(Long chatId, List<Integer> messageIds) {
//        userProjectMessages.put(chatId, messageIds);
//        log.debug("💾 Saved {} project message IDs for user: {}", messageIds.size(), chatId);
//    }
//
//    private void navigateToScreen(Long chatId, String screen, Integer messageId) {
//        log.debug("📱 Navigating to screen: {} for user {}", screen, chatId);
//
//        // Если screen уже содержит "navigation:back" - это ошибка, показываем главное меню
//        if (screen == null || screen.contains("navigation:back") || screen.trim().isEmpty()) {
//            showMainMenu(chatId, messageId);
//            return;
//        }
//
//        String[] screenParts = screen.split(":");
//        String screenType = screenParts[0];
//        String screenAction = screenParts.length > 1 ? screenParts[1] : "";
//        String screenParam = screenParts.length > 2 ? screenParts[2] : null;
//
//        log.debug("📱 Screen parsed - type: {}, action: {}, param: {}", screenType, screenAction, screenParam);
//
//        // Проверяем, что screenAction не пустой
//        if (screenAction.isEmpty()) {
//            log.warn("⚠️ Empty screen action for screen: {}, showing main menu", screen);
//            showMainMenu(chatId, messageId);
//            return;
//        }
//        switch (screenType) {
//            case "main":
//                showMainMenu(chatId, messageId);
//                break;
//            case "menu":
//                handleMenuAction(chatId, screenAction, messageId);
//                break;
//            case "project":
//                handleProjectAction(chatId, screenAction, screenParam, messageId);
//                break;
//            case "rules":
//                handleRulesAction(chatId, screenAction, "User", messageId);
//                break;
//            default:
//                log.warn("⚠️ Unknown screen type: {}, showing main menu", screenType);
//                showMainMenu(chatId, messageId);
//        }
//    }
//
//    private void showMainMenu(Long chatId, Integer messageId) {
//        // ПРИ ПОКАЗЕ ГЛАВНОГО МЕНЮ ОЧИЩАЕМ ВСЮ ИСТОРИЮ КРОМЕ ГЛАВНОГО ЭКРАНА
//        navigationService.resetToMain(chatId);
//        String text = "🔗 Добро пожаловать в TCMatch!\n\nВыберите действие:";
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
//        editMessage(chatId, messageId, text, keyboard);
//        log.info("📱 Showing main menu for user {}", chatId);
//    }
//
//    private void handleMenuAction(Long chatId, String action, Integer messageId) {
//        switch (action) {
//            case "profile":
//                showUserProfile(chatId, messageId);
//                break;
//            case "projects":
//                // ПРИ ПЕРЕХОДЕ В ПОИСК ОЧИЩАЕМ СТАРЫЕ ЭКРАНЫ ПОИСКА
//                navigationService.removeScreenOfType(chatId, "project");
//                showProjectsSearch(chatId, messageId, "");
//                break;
//            case "create_project":
//                showCreateProjectForm(chatId, messageId);
//                break;
//            case "browse_freelancers":
//                showFreelancersList(chatId, messageId);
//                break;
//            case "my_orders":
//                showMyOrders(chatId, messageId);
//                break;
//            case "help":
//                showHelp(chatId, messageId);
//                break;
//            case "about":
//                showAboutInfo(chatId, messageId);
//                break;
//            default:
//                log.warn("❌ Unknown menu action: {}", action);
//        }
//    }
//
//    public String getWelcomeText(Long chatId, String userName) {
//        if (!userService.userExists(chatId)) {
//            return """
//                    🔗 Добро пожаловать в TCMatch, %s!
//
//                    🚀 ПЛАТФОРМА ДЛЯ БЕЗОПАСНОЙ РАБОТЫ
//                    Разработчиков и Заказчиков
//
//                    💡 Для начала работы нажмите:
//                    "🚀 Начать регистрацию"
//
//                    🛡️ Ваша безопасность - наш приоритет!
//                    """.formatted(userName);
//        } else if (!userService.hasFullAccess(chatId)) {
//            RegistrationStatus status = userService.getRegistrationStatus(chatId);
//            return getRegistrationProgressText(userName, status);
//        } else {
//            return """
//                    🔗 С возвращением в TCMatch, %s!
//
//                    ✅ Регистрация завершена
//                    🚀 Выберите действие из меню
//                    """.formatted(userName);
//        }
//    }
//
//    private String getRegistrationProgressText(String userName, RegistrationStatus status) {
//        return switch (status) {
//            case REGISTERED -> """
//                🔗 С возвращением, %s!
//
//                ❗ Вы зарегистрированы, но ещё не ознакомились с правилами
//
//                📋 Следующий шаг:
//                Ознакомьтесь с правилами платформы
//                """.formatted(userName);
//
//            case RULES_VIEWED -> """
//                🔗 Рады снова видеть вас, %s!
//
//                ❗ Вы ознакомились с правилами
//
//                ✅ Финальный шаг:
//                Примите правила для завершения регистрации
//                """.formatted(userName);
//
//            default -> """
//                🔗 Добро пожаловать, %s!
//
//                ❗ Ваша регистрация не завершена
//                """.formatted(userName);
//        };
//    }
//
//    private void handleRegistrationAction(Long chatId, String action, String userName, Integer messageId) {
//        switch (action) {
//            case "start":
//                startRegistration(chatId, userName, messageId);
//                break;
//            default:
//                log.warn("❌ Unknown register action: {}", action);
//        }
//    }
//
//    private void handleRulesAction(Long chatId, String action, String userName, Integer messageId) {
//        switch (action) {
//            case "view":
//                showFullRules(chatId, messageId);
//                break;
//            case "accept":
//                acceptRules(chatId, userName, messageId);
//                break;
//            case "preview":
//                showRulesPreview(chatId, messageId);
//                break;
//            default:
//                log.warn("❌ Unknown rules action: {}", action);
//        }
//    }
//
//    private void startRegistration(Long chatId, String userName, Integer messageId) {
//        if (userService.userExists(chatId)) {
//            // Показываем текущий статус регистрации
//            RegistrationStatus status = userService.getRegistrationStatus(chatId);
//            String message = getRegistrationStatusMessage(status);
//            InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(status);
//            editMessage(chatId, messageId, message, keyboard);
//            return;
//        }
//
//        User user = userService.registerFromTelegram(chatId, userName, null, null);
//        String text = """
//            🚀 РЕГИСТРАЦИЯ НАЧАТА!
//
//            Добро пожаловать, %s!
//
//            📋 Следующий шаг:
//            Ознакомьтесь с правилами платформы
//            """.formatted(userName);
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(RegistrationStatus.REGISTERED);
//        editMessage(chatId, messageId, text, keyboard);
//        log.info("🚀 Registration started via callback for: {}", chatId);
//    }
//
//    private void showFullRules(Long chatId, Integer messageId) {
//        userService.markRulesViewed(chatId);
//
//        String rulesText = """
//                📜 ПРАВИЛА DEVLINK
//
//                1. 🛡️ Безопасность сделок
//                • Все платежи через защищенный Escrow-счет
//                • Деньги блокируются до подтверждения работы
//                • Исполнитель получает оплату после одобрения
//
//                2. 💰 Прозрачность оплаты
//                • Точный бюджет при создании проекта
//                • Все дополнительные работы через систему правок
//                • Без скрытых комиссий
//
//                3. ⏱️ Соблюдение сроков
//                • Исполнитель: уложиться в дедлайн
//                • Заказчик: проверить работу за 48 часов
//                • Авто-подтверждение через 2 дня
//
//                ✅ ДЛЯ ПОДТВЕРЖДЕНИЯ:
//                Нажмите "✅ Принять правила"
//                """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createRegistrationInProgressKeyboard(RegistrationStatus.RULES_VIEWED);
//        editMessage(chatId, messageId, rulesText, keyboard);
//    }
//
//    private void acceptRules(Long chatId, String userName, Integer messageId) {
//        User user = userService.acceptRules(chatId);
//
//        navigationService.removeScreenOfType(chatId, "rules");
//        navigationService.removeScreenOfType(chatId, "register");
//
//        String successText = """
//            🎉 РЕГИСТРАЦИЯ ЗАВЕРШЕНА!
//
//            %s, добро пожаловать в TCMatch!
//
//            ✅ Статус: %s
//            📅 Принято: %s
//
//            🚀 Теперь вам доступен полный функционал платформы
//            """.formatted(
//                userName,
//                user.getRegistrationStatus(),
//                user.getRulesAcceptedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
//        );
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
//        editMessage(chatId, messageId, successText, keyboard);
//
//        navigationService.resetToMain(chatId);
//
//        log.info("🎉 User completed registration via callback: {}", chatId);
//    }
//
//    private void showUserProfile(Long chatId, Integer messageId) {
//        if (!userService.hasFullAccess(chatId)) {
//            String message = "❌ Для доступа к профилю завершите регистрацию";
//            InlineKeyboardMarkup keyboard = keyboardFactory.getKeyboardForUser(chatId);
//            editMessage(chatId, messageId, message, keyboard);
//            return;
//        }
//
//        User user = userService.findByChatId(chatId).orElseThrow();
//
//        String profileText = """
//            👤 ВАШ ПРОФИЛЬ DEVLINK
//
//            📝 Имя: %s %s
//            🔗 Username: @%s
//            💼 Роль: %s
//            ⭐ Рейтинг: %.1f/5.0
//            📅 В системе с: %s
//
//            💡 Статистика:
//            • Завершенных сделок: 0
//            • Открытых проектов: 0
//            • Активных откликов: 0
//            """.formatted(
//                user.getFirstname(),
//                user.getLastname() != null ? user.getLastname() : "",
//                user.getUsername() != null ? user.getUsername() : "не указан",
//                user.getRole(),
//                user.getRating(),
//                user.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
//        );
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createBackButton();
//        editMessage(chatId, messageId, profileText, keyboard);
//    }
//
//    private String getRegistrationStatusMessage(RegistrationStatus status) {
//        return switch (status) {
//            case REGISTERED -> "⚠️ ВЫ УЖЕ НАЧАЛИ РЕГИСТРАЦИЮ\n\nСледующий шаг:\nОзнакомьтесь с правилами платформы";
//            case RULES_VIEWED ->  "⚠️ ВЫ УЖЕ ОЗНАКОМИЛИСЬ С ПРАВИЛАМИ\n\nФинальный шаг:\nПримите правила для завершения регистрации";
//            case RULES_ACCEPTED -> "✅ Вы уже завершили регистрацию!";
//            default -> "❌ Ошибка статуса";
//        };
//    }
//
//    private void showRulesPreview(Long chatId, Integer messageId) {
//        String previewText = """
//            📋 ОСНОВНЫЕ ПРАВИЛА DEVLINK
//
//            🛡️ Безопасность:
//            • Все платежи через Escrow-систему
//            • Гарантия оплаты для исполнителей
//            • Гарантия качества для заказчиков
//
//            💰 Прозрачность:
//            • Фиксированные бюджеты проектов
//            • Без скрытых комиссий
//            • Мгновенные выплаты
//
//            ⚠️ Полные правила будут доступны после регистрации
//            """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createUnauthorizedUserKeyboard();
//        editMessage(chatId, messageId, previewText, keyboard);
//    }
//
//    private void showAboutInfo(Long chatId, Integer messageId) {
//        String aboutText = """
//            ℹ️ О ПРОЕКТЕ DEVLINK
//
//            🚀 Платформа для безопасной работы
//            разработчиков и заказчиков
//
//            💡 Наша миссия:
//            Создать экосистему, где каждая сторона
//            защищена и уверена в результате
//            """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createUnauthorizedUserKeyboard();
//        editMessage(chatId, messageId, aboutText, keyboard);
//    }
//
//    private void showProjectsList(Long chatId, Integer messageId) {
//        String text = "🚧 Раздел проектов в разработке...";
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void showCreateProjectForm(Long chatId, Integer messageId) {
//        String text = "🚧 Создание проектов в разработке...";
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void showFreelancersList(Long chatId, Integer messageId) {
//        String text = "🚧 Поиск исполнителей в разработке...";
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void showMyOrders(Long chatId, Integer messageId) {
//        String text = "🚧 Раздел заказов в разработке...";
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMainMenuKeyboard();
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void showHelp(Long chatId, Integer messageId) {
//        String helpText = """
//            🆘 Помощь по TCMatch
//
//            💡 Основные возможности:
//            • Безопасные сделки с Escrow
//            • Поиск проектов и исполнителей
//            • Система рейтингов и отзывов
//
//            🚀 Скоро появится:
//            • Создание проектов
//            • Система платежей
//            • Чат между участниками
//            """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.getKeyboardForUser(chatId);
//        editMessage(chatId, messageId, helpText, keyboard);
//    }
//
//     //Отправить сообщение с ошибкой
//
//    private void sendErrorMessage(Long chatId, String errorText) {
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId.toString());
//        message.setText("❌ " + errorText);
//
//        try {
//            sender.execute(message);
//        } catch (TelegramApiException e) {
//            log.error("❌ Error sending error message: {}", e.getMessage());
//        }
//    }
//    private void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
//        EditMessageText editMessage = new EditMessageText();
//        editMessage.setChatId(chatId.toString());
//        editMessage.setMessageId(messageId);
//        editMessage.setText(text);
//        editMessage.setReplyMarkup(keyboard);
//
//        try {
//            sender.execute(editMessage);
//            log.info("✅ Message edited for: {}", chatId);
//        } catch (TelegramApiException e) {
//            log.error("❌ Error editing message: {}", e.getMessage());
//        }
//    }
//
//    private void handleOrderAction(Long chatId, String action, String parameter, Integer messageId) {
////        switch (action) {
////            case "view":
////                showOrderDetails(chatId, Long.parseLong(parameter), messageId);
////                break;
////            case "list":
////                showUserOrders(chatId, messageId);
////                break;
////            case "start":
////                startOrder(chatId, Long.parseLong(parameter), messageId);
////                break;
////            case "submit":
////                showSubmitWorkForm(chatId, Long.parseLong(parameter), messageId);
////                break;
////            case "accept":
////                acceptWork(chatId, Long.parseLong(parameter), messageId);
////                break;
////            case "revision":
////                showRevisionForm(chatId, Long.parseLong(parameter), messageId);
////                break;
////            case "resolve_revision":
////                resolveRevision(chatId, Long.parseLong(parameter), messageId);
////                break;
////
////        }
//    }
//
//    private void handleProjectAction(Long chatId, String action, String parameter, Integer messageId) {
//        switch (action) {
//            case "search":
//                String searchScreen = "project:search:" + (parameter != null ? parameter: "");
//
//                String currentScreen = navigationService.getCurrentScreen(chatId);
//
//                if (searchScreen.equals(currentScreen)) {
//                    log.debug("📱 Already on search screen, skipping");
//                    return; // Уже на этом экране - выходим
//                }
//
//                navigationService.pushScreen(chatId, searchScreen);
//
//                showProjectsSearch(chatId, messageId, parameter != null ? parameter : "");
//                break;
//            case "filters":
//                showSearchFilters(chatId, messageId, parameter != null ? parameter : "");
//                break;
//            case"filter":
//                applyFilter(chatId, messageId, parameter);
//                break;
//            case "page":
//                handlePageNavigation(chatId, parameter, messageId);
//                break;
//            case "details":
//                showProjectDetails(chatId, Long.parseLong(parameter), messageId);
//                break;
//            case "apply":
//                showApplyForm(chatId, Long.parseLong(parameter), messageId);
//                break;
//        }
//    }
//
//    private void showProjectDetails(Long chatId, Long projectId, Integer messageId) {
//        String text = "🚧 Детали проекта в разработке...\n\nID проекта: " + projectId;
//        InlineKeyboardMarkup keyboard = keyboardFactory.createBackButton();
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void showApplyForm(Long chatId, Long projectId, Integer messageId) {
//        String text = "🚧 Подача заявки в разработке...\n\nID проекта: " + projectId;
//        InlineKeyboardMarkup keyboard = keyboardFactory.createBackButton();
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void showProjectsSearch(Long chatId, Integer messageId, String filter) {
//        try {
//            String safeFilter = filter != null ? filter : "";
//            List<Project> pageProjects = projectSearchService.getPageProjects(chatId, safeFilter);
//            ProjectSearchService.SearchState stage = projectSearchService.getOrCreateSearchState(chatId, safeFilter);
//
//            if (pageProjects.isEmpty()) {
//                String text = "🔍 Проекты не найдены\n\nПопробуйте изменить фильтры поиска";
//
//                InlineKeyboardMarkup keyboard = keyboardFactory.createSearchFiltersKeyboard(safeFilter);
//                editMessage(chatId, messageId, text, keyboard);
//                return;
//            }
//
//            // УДАЛЯЕМ предыдущие сообщения с проектами (если есть)
//            deletePreviousProjectMessages(chatId);
//
//            // ОТПРАВЛЯЕМ новые сообщения с проектами
//            List<Integer> newMessageIds = new ArrayList<>();
//            for (int i = 0; i < pageProjects.size(); i++) {
//                Project project = pageProjects.get(i);
//                String projectText = formatProjectPreview(project, i + 1);
//                InlineKeyboardMarkup projectKeyboard = keyboardFactory.createProjectPreviewKeyboard(project.getId());
//
//                Integer newMessageId = sendInlineMessageReturnId(chatId, projectText, projectKeyboard);
//                newMessageIds.add(newMessageId);
//            }
//
//            // Сохраняем IDs новых сообщений для будущего удаления
//            saveProjectMessageIds(chatId, newMessageIds);
//
//            // Показываем пагинацию и фильтры
//            String paginationText = createPaginationText(chatId);
//            InlineKeyboardMarkup paginationKeyboard = keyboardFactory.createPaginationKeyboard(filter, chatId);
//
//            editMessage(chatId, messageId, paginationText, paginationKeyboard);
//
//        } catch (Exception e) {
//            log.error("❌ Error showing projects search: {}", e.getMessage());
//            sendErrorMessage(chatId, "Ошибка при поиске проектов");
//        }
//    }
//
//    private void showSearchFilters(Long chatId, Integer messageId, String currentFilter) {
//        String text = "⚙️ **ФИЛЬТРЫ ПОИСКА**\n\nВыберите критерии поиска:";
//        InlineKeyboardMarkup keyboard = keyboardFactory.createSearchFiltersKeyboard(currentFilter);
//        editMessage(chatId, messageId, text, keyboard);
//    }
//
//    private void applyFilter(Long chatId, Integer messageId, String filter) {
//        // Просто обновляем поиск с новым фильтром
//        String safeFilter = filter != null ? filter : "";
//        showProjectsSearch(chatId, messageId, safeFilter);
//    }
//
//    private void handlePageNavigation(Long chatId, String parameter, Integer messageId) {
//        String[] parts = parameter.split(":");
//        String direction = parts[0];
//        String filter = parts.length > 1 ? parts[1] : "";
//
//        if ("next".equals(direction)) {
//            projectSearchService.nextPage(chatId);
//        } else if ("prev".equals(direction)) {
//            projectSearchService.prevPage(chatId);
//        }
//
//        // Обновляем отображение
//        showProjectsSearch(chatId, messageId, filter);
//    }
//
//    private String formatProjectPreview(Project project, int number) {
//        return """
//            🎯 **Проект #%d**
//
//            💼 *%s*
//            💰 Бюджет: *%.0f руб*
//            ⏱️ Срок: *%d дней*
//            👀 Просмотров: *%d*
//            📨 Откликов: *%d*
//
//            📝 %s
//            """.formatted(
//                number,
//                project.getTitle(),
//                project.getBudget(),
//                project.getEstimatedDays(),
//                project.getViewsCount(),
//                project.getApplicationsCount(),
//                project.getDescription().length() > 100 ?
//                        project.getDescription().substring(0, 100) + "...":
//                        project.getDescription()
//        );
//    }
//
//    private String createPaginationText(Long chatId) {
//        int currentPage = projectSearchService.getCurrentPage(chatId);
//        int totalPages = projectSearchService.getTotalPages(chatId);
//        return "📄 **Страница %d из %d**\n\nИспользуйте кнопки ниже для навигации:".formatted(currentPage + 1, totalPages);
//    }
//
//    private Integer sendInlineMessageReturnId(Long chatId, String text, InlineKeyboardMarkup keyboard) {
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId.toString());
//        message.setText(text);
//        message.setReplyMarkup(keyboard);
//
//        try {
//            org.telegram.telegrambots.meta.api.objects.Message sentMessage = sender.execute(message);
//            return sentMessage.getMessageId();
//        } catch (TelegramApiException e) {
//            log.error("❌ Error sending message: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    private void deleteMessage(Long chatId, Integer messageId) {
//        if (messageId == null) return;
//
//        DeleteMessage deleteMessage = new DeleteMessage();
//        deleteMessage.setChatId(chatId.toString());
//        deleteMessage.setMessageId(messageId);
//
//        try {
//            sender.execute(deleteMessage);
//        } catch (TelegramApiException e) {
//            log.error("❌ Error deleting message: {}", e.getMessage());
//        }
//    }
//
//    private void handleNavigationAction(Long chatId, String action, String parameter, Integer messageId) {
//        if ("back".equals(action)) {
//            String previousScreen = navigationService.popScreen(chatId);
//            log.info("📱 Navigation back: {} -> {}", chatId, previousScreen);
//
//            navigateToScreen(chatId, previousScreen, messageId);
//        }
//    }
//
//    private void deletePreviousProjectMessages(Long chatId) {
//        // Удаляем предыдущие сообщения с проектами
//        List<Integer> previousMessageIds = getSavedProjectMessageIds(chatId);
//
//        if (previousMessageIds!= null && !previousMessageIds.isEmpty()) {
//            log.debug("🗑️ Deleting {} project messages for user {}", previousMessageIds.size(), chatId);
//            for (Integer msgId : previousMessageIds) {
//                deleteMessage(chatId, msgId);
//            }
//
//            clearSavedProjectMessageIds(chatId);
//        }
//    }
//
//    //Получить сохраненные ID сообщений с проектами для пользователя
//    private List<Integer> getSavedProjectMessageIds(Long chatId) {
//        return userProjectMessages.getOrDefault(chatId, new ArrayList<>());
//    }
//
//    //Очистить сохраненные ID сообщений с проектами для пользователя
//    private void clearSavedProjectMessageIds(Long chatId) {
//        userProjectMessages.remove(chatId);
//        log.debug("🗑️ Cleared project message IDs for user: {}", chatId);
//    }
}
