package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.TCMatchBot;
import com.tcmatch.tcmatch.model.dto.PaginationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaginationManager {

    private final UserSessionService userSessionService;
    private final TCMatchBot bot;

    // КЛЮЧ: Универсальный префикс для хранения контекста в сессии
    private static final String CONTEXT_PREFIX = "PAGINATION_CTX_";


    // -----------------------------------------------------------------
    // 1. ГЛАВНЫЙ МЕТОД РЕНДЕРИНГА
    // -----------------------------------------------------------------
    public void renderIdBasedPage(
            Long chatId,
            String contextKey,       // "favorites", "search", "my_applications"
            List<Long> entityIds,    // 🔥 ID проектов или откликов
            String entityType,       // "PROJECT" или "APPLICATION"
            String direction,        // "init", "next", "prev"
            int pageSize,
            BiFunction<List<Long>, PaginationContext, List<Integer>> renderer
    ) {
        String sessionKey = CONTEXT_PREFIX + contextKey;
        PaginationContext currentContext = userSessionService.getFromContext(chatId, sessionKey, PaginationContext.class);

        // 1. ИНИЦИАЛИЗАЦИЯ
        if ("init".equals(direction) || currentContext == null) {
            if (entityIds == null || entityIds.isEmpty()) {
                log.warn("⚠️ Пустой список ID для пагинации {}: {}", chatId, contextKey);
                handleEmptyResults(chatId, contextKey);
                return;
            }

            // 🔥 СОЗДАЕМ КОНТЕКСТ В ЗАВИСИМОСТИ ОТ ТИПА
            if ("PROJECT".equals(entityType)) {
                currentContext = PaginationContext.forProjects(chatId, entityIds, contextKey, pageSize);
            } else {
                currentContext = PaginationContext.forApplications(chatId, entityIds, contextKey, pageSize);
            }

            userSessionService.putToContext(chatId, sessionKey, currentContext);
            log.debug("🔄 Инициализирована пагинация {}: {} {} элементов",
                    contextKey, entityIds.size(), entityType.toLowerCase());
        }

        // 2. РАСЧЕТ НОВОЙ СТРАНИЦЫ
        int newPage = calculateNewPage(currentContext, direction);

        // Если страница не изменилась - выходим
        if (newPage == currentContext.currentPage() && !"init".equals(direction)) {
            return;
        }

        // 3. 🔥 УДАЛЯЕМ СТАРЫЕ СООБЩЕНИЯ (Централизованно через UserSessionService)
        // 1. Получаем ID и очищаем сессию в одном вызове из сервиса
        List<Integer> messageIds = userSessionService.getAndClearTemporaryMessageIds(chatId);
        // 2. Если ID есть, используем BotExecutor для отправки команды
        if (!messageIds.isEmpty()) {
            // 🔥 Вызов нового метода на BotExecutor
            bot.deleteMessages(chatId, messageIds);
        }

        // 4. 🔥 ПОЛУЧАЕМ ID ДЛЯ НОВОЙ СТРАНИЦЫ (используем newPage, а не currentContext)
        // 🔥 ИСПРАВЛЕННЫЙ БЛОК: 4. ПОЛУЧАЕМ ID И РАССЧИТЫВАЕМ ИНДЕКСЫ
        List<Long> idsInContext = currentContext.entityIds();

        int totalSize = idsInContext.size();

        // pageSize у вас приходит как параметр, используем его
        // int pageSize = pageSize; // pageSize уже доступен

        int startIndex = newPage * pageSize;
        // 🔥 Ключевое исправление: Math.min гарантирует, что endIndex не выйдет за пределы списка
        int endIndex = Math.min(startIndex + pageSize, totalSize);

        if (startIndex >= totalSize || startIndex < 0 || startIndex >= endIndex) {
            log.warn("⚠️ Пустая страница или некорректный индекс {}: страница {}", contextKey, newPage);
            // Для надежности можно здесь вернуть на первую страницу (newPage = 0)
            return;
        }

        List<Long> pageIds = idsInContext.subList(startIndex, endIndex);

        if (pageIds.isEmpty()) {
            log.warn("⚠️ Пустая страница для пагинации {}: страница {}", contextKey, newPage);
            return;
        }

        // 6. СОХРАНЯЕМ НОВЫЙ КОНТЕКСТ
        PaginationContext newContext = currentContext.withNewPage(newPage);

        // 6. 🔥 РЕНДЕРИНГ (Возвращает ID новых сообщений)
        List<Integer> newMessageIds = renderer.apply(pageIds, newContext);

        // 7. 🔥 СОХРАНЯЕМ НОВЫЕ ID СООБЩЕНИЙ В СЕССИЮ
        if (newMessageIds != null) {
            for (Integer messageId : newMessageIds) {
                userSessionService.addTemporaryMessageId(chatId, messageId);
            }
        }

        userSessionService.putToContext(chatId, sessionKey, newContext);

        log.debug("📄 Отрендерена страница {}: {} {}/{}",
                contextKey, newPage + 1, newContext.getTotalPages(), entityType);
    }

    private int calculateNewPage(PaginationContext context, String direction) {
        int currentPage = context.currentPage();

        if ("next".equals(direction) && context.hasNextPage()) {
            return currentPage + 1;
        } else if ("prev".equals(direction) && context.hasPreviousPage()) {
            return currentPage - 1;
        }
        return currentPage;
    }

    private void handleEmptyResults(Long chatId, String contextKey) {
        // 🔥 РАЗНЫЕ СООБЩЕНИЯ ДЛЯ РАЗНЫХ КОНТЕКСТОВ
        String message = switch (contextKey) {
            case "favorites" -> "⭐ У вас пока нет избранных проектов";
            case "search" -> "🔍 По вашему запросу ничего не найдено";
            case "my_applications" -> "📨 У вас пока нет откликов";
            default -> "📭 Ничего не найдено";
        };

        bot.sendTemporaryErrorMessage(chatId, message, 5);
    }

    // -----------------------------------------------------------------
    // 2. МЕТОДЫ-ХЕЛПЕРЫ
    // -----------------------------------------------------------------

    private void deleteOldMessages(Long chatId, List<Integer> messageIds) {
        if (messageIds != null) {
            for (Integer id : messageIds) {
                bot.deleteMessage(chatId, id);
            }
        }
    }
}
