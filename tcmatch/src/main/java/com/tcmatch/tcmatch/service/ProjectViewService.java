package com.tcmatch.tcmatch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectViewService {

    private final ProjectService projectService;

    private final Map<Long, Map<Long, LocalDateTime>> userProjectViews = new ConcurrentHashMap<>();

    // 🔥 ВРЕМЯ МЕЖДУ ПРОСМОТРАМИ ОДНОГО ПРОЕКТА (30 минут)
    private static final int VIEW_COOLDOWN_MINUTES = 30;

    // 🔥 РЕГИСТРАЦИЯ ПРОСМОТРА ПРОЕКТА
    @Transactional
    public void registerProjectView(Long chatId, Long projectId) {
        try {
            // 🔥 ПРОВЕРЯЕМ, НЕ СЛИШКОМ ЛИ ЧАСТО ПОЛЬЗОВАТЕЛЬ СМОТРИТ ЭТОТ ПРОЕКТ
            if (canUserViewProject(chatId, projectId)) {
                // 🔥 УВЕЛИЧИВАЕМ СЧЕТЧИК ПРОСМОТРОВ
                projectService.incrementProjectViews(projectId);

                // 🔥 СОХРАНЯЕМ ВРЕМЯ ПОСЛЕДНЕГО ПРОСМОТРА
                recordUserView(chatId, projectId);

                log.debug("👀 Зарегистрирован просмотр проекта {} пользователем {}", projectId, chatId);
            } else {
                log.debug("⏳ Пользователь {} слишком часто смотрит проект {}", chatId, projectId);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка регистрации просмотра проекта: {}", e.getMessage());
        }
    }

    private boolean canUserViewProject(Long chatId, Long projectId) {
         Map<Long, LocalDateTime> userViews = userProjectViews.get(chatId);

        if (userViews == null) {
            return true; // Первый просмотр этого проекта
        }

        LocalDateTime lastView = userViews.get(projectId);
        if (lastView == null) {
            return true; // Первый просмотр этого проекта
        }

        // 🔥 ПРОВЕРЯЕМ, ПРОШЛО ЛИ ДОСТАТОЧНО ВРЕМЕНИ С ПОСЛЕДНЕГО ПРОСМОТРА
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownEnd = lastView.plusMinutes(VIEW_COOLDOWN_MINUTES);

        return now.isAfter(cooldownEnd);
    }

    // 🔥 ЗАПИСЬ ПРОСМОТРА ПОЛЬЗОВАТЕЛЕМ
    private void recordUserView(Long chatId, Long projectId) {
        userProjectViews
                .computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
                .put(projectId, LocalDateTime.now());
    }

    // 🔥 ОЧИСТКА СТАРЫХ ЗАПИСЕЙ (можно вызывать по расписанию)
    @Scheduled(fixedRate = 7200000) // Каждый 2 час
    public void cleanupOldViews() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // 24 часа
        int initialSize = userProjectViews.size();

        userProjectViews.entrySet().removeIf(entry -> {
            entry.getValue().values().removeIf(viewTime -> viewTime.isBefore(cutoffTime));
            return entry.getValue().isEmpty();
        });

        int finalSize = userProjectViews.size();
        if (initialSize != finalSize) {
            log.info("🧹 Очистка старых просмотров: {} -> {} пользователей", initialSize, finalSize);
        }
    }
}
