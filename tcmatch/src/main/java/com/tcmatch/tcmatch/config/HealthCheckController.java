package com.tcmatch.tcmatch.config;

import com.tcmatch.tcmatch.service.ShutdownService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
public class HealthCheckController {

    private final ShutdownService shutdownService;

    /**
     * 🔥 Ручной graceful shutdown
     */
    @PostMapping("/shutdown")
    public ResponseEntity<String> gracefulShutdown() {
        log.warn("🛑 ИНИЦИИРОВАН РУЧНОЙ SHUTDOWN ПОЛЬЗОВАТЕЛЕМ!");

        new Thread(() -> {
            try {
                // 1. Очищаем все сообщения
                shutdownService.cleanupAllUserMessages();

                // 2. Ждем немного
                Thread.sleep(2000);

                // 3. Завершаем приложение
                log.info("🚀 Завершение работы приложения...");
                System.exit(0);

            } catch (Exception e) {
                log.error("❌ Ошибка при graceful shutdown: {}", e.getMessage(), e);
            }
        }).start();

        return ResponseEntity.ok("Graceful shutdown initiated");
    }

    /**
     * 🔥 Проверка здоровья приложения
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "TC Match Bot");
        return ResponseEntity.ok(health);
    }
}