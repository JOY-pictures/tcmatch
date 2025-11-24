//package com.tcmatch.tcmatch.bot.handlers;
//
//
//import com.tcmatch.tcmatch.bot.BotExecutor;
//import com.tcmatch.tcmatch.model.Application;
//import com.tcmatch.tcmatch.model.Project;
//import com.tcmatch.tcmatch.model.dto.PaginationContext;
//import com.tcmatch.tcmatch.model.dto.ProjectData;
//import com.tcmatch.tcmatch.service.PaginationManager;
//import com.tcmatch.tcmatch.service.UserSessionService;
//import com.tcmatch.tcmatch.util.PaginationContextKeys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.BiFunction;
//
//@Component
//@Slf4j
//public class CallbackHandler {
//
//    private final UserSessionService userSessionService;
////    private final List<BaseHandler> handlers;
//    private final BotExecutor botExecutor;
//
//    private final ApplicationHandler applicationHandler;
//    private final ProjectsHandler projectsHandler;
//    private final PaginationManager paginationManager;
//
//    public CallbackHandler(UserSessionService userSessionService, BotExecutor botExecutor,
//                           @Lazy ApplicationHandler applicationHandler, @Lazy ProjectsHandler projectsHandler,
//                           PaginationManager paginationManager) {
////        this.handlers = handlers;
//        this.userSessionService = userSessionService;
//        this.botExecutor = botExecutor;
//        this.applicationHandler = applicationHandler;
//        this.projectsHandler = projectsHandler;
//        this.paginationManager = paginationManager;
//    }
//
//    public void handleCallback(Long chatId, String callbackData, String userName, Integer messageId) {
//
//
//        log.info("🔄 Handling callback: {} from user {}", callbackData, chatId);
//
//        // 1. ПАРСИНГ ДАННЫХ
//        String[] parts = callbackData.split(":", 3);
//        String actionType = parts[0];
//        String action = parts[1];
//        String parameter = parts.length > 2 ? parts[2] : null;
//
//        // 2. 🔥 СОЗДАНИЕ DTO ДЛЯ НОВЫХ МЕТОДОВ
//        ProjectData data = new ProjectData(chatId, messageId, userName, null, actionType, action);
//        // Добавляем параметр в DTO, если он есть (хотя handlePaginationCallback парсит его сам)
//        // data.setParameter(parameter);
//
//        saveToNavigationHistory(chatId, actionType, action, parameter);
//
//        // 🔥 НОВОЕ ИДЕАЛЬНОЕ УСЛОВИЕ:
//        if ("pagination".equals(actionType)) {
//
//            // Пересобираем полный параметр в формат "next:my_projects:PROJECT"
//            // Мы знаем, что contextKey (parameter) содержит информацию о типе сущности
//            String fullParameterForPagination = action + ":" + parameter + ":" +
//                    (parameter.contains("project") ? "PROJECT" : "APPLICATION");
//
//            // 🔥 ВЫЗОВ ВАШЕГО ДИСПЕТЧЕРА
//            handlePaginationCallback(data, fullParameterForPagination);
//
//            // Выходим, чтобы избежать делегирования в ProjectsHandler
//            return;
//        }
//
////        // 4. ОСНОВНОЙ ЦИКЛ: ДЕЛЕГИРОВАНИЕ ОБРАБОТЧИКАМ (canHandle/handle)
////        for (BaseHandler handler : handlers) {
////            if (handler.canHandle(actionType, action)) {
////                // Если хендлер найден, вызываем его
////                handler.handle(chatId, action, parameter, messageId, userName);
////                return;
////            }
////        }
//
//        // Если не нашли обработчик
//        log.warn("⚠️ No handler found for: {}:{}", actionType, action);
//    }
//
//
//    //Сохраняет действие в историю навигации (если нужно)
//    private void saveToNavigationHistory(Long chatId, String actionType, String action, String parameter) {
//
//        System.out.println(userSessionService.getUserHistory(chatId));
//
//        // 🔥 ПОЛУЧАЕМ ТЕКУЩИЙ ЭКРАН ИЗ СЕССИИ
//        String currentScreen = userSessionService.getFromContext(chatId, "currentScreen", String.class);
//
//
//        System.out.println(userSessionService.getUserHistory(chatId));
//
//
//        // 🚫 НЕ СОХРАНЯЕМ ТЕКУЩИЙ ЭКРАН ПРИ НАВИГАЦИИ "НАЗАД"
//        if ("navigation".equals(actionType) && "back".equals(action)) {
//            log.debug("📱 Skipping history save for BACK navigation");
//            return;
//        }
//
//        //🔥 ПЕРЕХОД НА ГЛАВНЫЙ ЭКРАН - СБРАСЫВАЕМ ИСТОРИЮ
//        if ("menu".equals(actionType) && "main".equals(action)) {
//            log.debug("📱 Reset history for MAIN menu navigation");
//            userSessionService.resetToMain(chatId); // 🔥 СБРАСЫВАЕМ ИСТОРИЮ
//            return;
//        }
//
//        if ("project".equals(actionType) && "favorite".equals(action)) {
//            log.debug("📱 Skipping history save for favorite");
//            return;
//        }
//
//        // 🔥 ЕСЛИ ЭТО ТОТ ЖЕ ACTION - ПРОСТО ОБНОВЛЯЕМ КОНТЕКСТ БЕЗ СОХРАНЕНИЯ
//        if (currentScreen != null && isSameAction(currentScreen, action)) {
//            log.debug("📱 Same action {} - updating context without history", action);
//            String newScreen = actionType + ":" + action + (parameter != null ? ":" + parameter : "");
//            userSessionService.putToContext(chatId, "currentScreen", newScreen);
//            return;
//        }
//
//        String newScreen = actionType + ":" + action + (parameter != null ? ":" + parameter : "");
//
//
//        // ✅ РАЗНЫЙ ACTION - СОХРАНЯЕМ ТЕКУЩИЙ В ИСТОРИЮ
//        if (currentScreen != null && !currentScreen.isEmpty()) {
//                userSessionService.pushToNavigationHistory(chatId, currentScreen);
//                log.debug("📱 Saved current screen to history: {}", currentScreen);
//        }
//
//        // 🔥 ОБНОВЛЯЕМ ТЕКУЩИЙ ЭКРАН НА НОВЫЙ
//        userSessionService.putToContext(chatId, "currentScreen", newScreen);
//        log.debug("📱 Updated current screen: {}", newScreen);
//    }
//
//    /**
//     * Определяет, является ли действие внутренней командой, которую нужно игнорировать (не сохранять).
//     * Возвращает true для действий, которые не являются полноценными экранами.
//     */
//    private boolean isNonNavigableAction(String actionType, String action, String parameter) {
//        // 1. Пагинация
//        if (PaginationContextKeys.PREFIX_PAGINATION_NEXT.equals(action) ||
//                PaginationContextKeys.PREFIX_PAGINATION_PREV.equals(action) ||
//                // Пагинация может быть в формате projects:pagination:next или просто pagination:next (если у вас есть)
//                "pagination".equals(action)) {
//            return true;
//        }
//
//        // 2. Команды подтверждения/применения (projects:filter:apply)
//        // Строим полный колбэк для точной проверки
//        String fullCallback = actionType + ":" + action + (parameter != null ? ":" + parameter : "");
//        if (PaginationContextKeys.CALLBACK_PROJECTS_FILTER_APPLY.equals(fullCallback)) {
//            return true;
//        }
//
//        // 3. Действия над сущностями (Accept/Reject/Delete/Withdraw)
//        if (PaginationContextKeys.PREFIX_ACTION_ACCEPT.equals(action) ||
//                PaginationContextKeys.PREFIX_ACTION_REJECT.equals(action) ||
//                PaginationContextKeys.PREFIX_ACTION_WITHDRAW.equals(action) ||
//                PaginationContextKeys.PREFIX_ACTION_DELETE.equals(action)) {
//            return true;
//        }
//
////        // 4. Главные меню (menu:projects, menu:applications, main)
////        if (PaginationContextKeys.PREFIX_MENU.equals(actionType) || "main".equals(actionType)) {
////            return true;
////        }
//
//        return false;
//    }
//
//    // 🔥 ПРОВЕРЯЕМ, ЭТО ТОТ ЖЕ ACTION (просто разные параметры)
//    private boolean isSameAction(String currentScreen, String newAction) {
//        if (currentScreen == null) return false;
//
//        // 🔥 ИЗВЛЕКАЕМ ACTION ИЗ ТЕКУЩЕГО ЭКРАНА
//        String[] parts = currentScreen.split(":");
//        if (parts.length >= 2) {
//            String currentAction = parts[1]; // filter, pagination, search и т.д.
//            return currentAction.equals(newAction);
//        }
//        return false;
//    }
//
//    /**
//     * Вспомогательный метод-диспетчер для всех callback-запросов, связанных с пагинацией (prev/next).
//     */
//    private void handlePaginationCallback(ProjectData data, String parameter) {
//        try {
//            // 🔥 НОВЫЙ ФОРМАТ ПАРАМЕТРА: "next:favorites:PROJECT" или "prev:my_applications:APPLICATION"
//            String[] parts = parameter.split(":");
//            if (parts.length < 3) {
//                log.error("❌ Неверный формат параметра пагинации: {}", parameter);
//                return;
//            }
//
//            String direction = parts[0];   // "next" или "prev"
//            String contextKey = parts[1];  // "favorites", "search", "my_applications"
//            String entityType = parts[2];  // "PROJECT" или "APPLICATION"
//
//            log.debug("🔄 Обработка пагинации: direction={}, context={}, type={}",
//                    direction, contextKey, entityType);
//
//            // 🔥 ОПРЕДЕЛЯЕМ РЕНДЕРЕР И РАЗМЕР СТРАНИЦЫ ДЛЯ КАЖДОГО КОНТЕКСТА
//            switch (contextKey) {
//                case "favorites":
//                case "project_search":
//                case "my_projects":
//                case "PROJECT":
//                    handleProjectPagination(data, direction, contextKey, entityType);
//                    break;
//                case "my_applications":
//                case "APPLICATION":
////                    handleApplicationPagination(data, direction, contextKey, entityType);
//                    break;
//                case "customer_projects":
//                default:
//                    log.error("❌ Неизвестный контекст пагинации: {}", contextKey);
//            }
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка обработки пагинации: {}", parameter, e);
//        }
//    }
//
//    /**
//     * 🔥 ПАГИНАЦИЯ ДЛЯ ПРОЕКТОВ (избранные, поиск, мои проекты)
//     */
//    private void handleProjectPagination(ProjectData data, String direction, String contextKey, String entityType) {
//        try {
//            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = null;
//            int pageSize = 5; // PROJECTS_PER_PAGE
//
//            // 🔥 ОПРЕДЕЛЯЕМ РЕНДЕРЕР ДЛЯ КАЖДОГО КОНТЕКСТА
//            switch (contextKey) {
//                case "favorites":
//                    renderer = projectsHandler::renderFavoritesPage;
//                    break;
//                case "project_search":
//                    renderer = projectsHandler::renderSearchPage;
//                    break;
//                case "customer_projects":
//                    renderer = projectsHandler::renderCustomerProjectsPage;
//                    break;
////                case "my_projects":
////                    renderer = (ids, context) -> projectsHandler.renderMyProjectsPage(
////                            ids, context,
////                            userSessionService.getFromContext(data.getChatId(), "my_projects_filter", String.class)
////                    );
////                    pageSize = 3; // специальный размер для моих проектов
////                    break;
//            }
//
//            if (renderer == null) {
//                log.error("❌ Renderer not found for project context: {}", contextKey);
//                return;
//            }
//
//            // 🔥 ВЫЗЫВАЕМ НОВЫЙ МЕТОД С ID
//            paginationManager.renderIdBasedPage(
//                    data.getChatId(),
//                    contextKey,
//                    null, // ID уже в контексте сессии
//                    entityType,
//                    direction,
//                    pageSize,
//                    renderer
//            );
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка пагинации проектов: {}", contextKey, e);
//        }
//    }
//
//    /**
//     * 🔥 ПАГИНАЦИЯ ДЛЯ ОТКЛИКОВ
//     */
////    private void handleApplicationPagination(ProjectData data, String direction, String contextKey, String entityType) {
////        try {
////            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer = null;
////
////            // 🔥 ПРАВИЛЬНО ОПРЕДЕЛЯЕМ РЕНДЕРЕР ДЛЯ КАЖДОГО КОНТЕКСТА ОТКЛИКОВ
////            switch (contextKey) {
////                case "my_applications":
////                    renderer = applicationHandler.getFreelancerApplicationsRenderer();
////                    break;
////                case "project_applications":
////                    renderer = applicationHandler.getProjectApplicationsRenderer();
////                    break;
////                default:
////                    log.error("❌ Unknown application context: {}", contextKey);
////                    return;
////            }
////
////            if (renderer == null) {
////                log.error("❌ Renderer not found for application context: {}", contextKey);
////                return;
////            }
////
////            // 🔥 ВЫЗЫВАЕМ PAGINATION MANAGER С ПРАВИЛЬНЫМИ ПАРАМЕТРАМИ
////            paginationManager.renderIdBasedPage(
////                    data.getChatId(),
////                    contextKey,
////                    null, // ID уже в контексте сессии
////                    entityType,
////                    direction,
////                    applicationHandler.getApplicationsPerPage(),
////                    renderer
////            );
////
////        } catch (Exception e) {
////            log.error("❌ Ошибка пагинации откликов: {}", contextKey, e);
////        }
////    }
//}