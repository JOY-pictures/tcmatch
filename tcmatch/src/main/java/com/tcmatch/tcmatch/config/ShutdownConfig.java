package com.tcmatch.tcmatch.config;

import com.tcmatch.tcmatch.service.ShutdownService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ShutdownConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final ShutdownService shutdownService;

    /**
     * 🔥 Регистрируем shutdown hook при запуске приложения
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("🚨 ВЫЗВАН SHUTDOWN HOOK (SIGTERM, Ctrl+C, System.exit())");
            shutdownService.cleanupAllUserMessages();
        }));

        log.info("✅ Shutdown hook зарегистрирован");
    }

    /**
     * 🔥 Вызывается перед уничтожением контекста Spring
     */
    @PreDestroy
    public void onDestroy() {
        log.warn("🔥 ВЫЗВАН @PreDestroy (закрытие контекста Spring)");
        // shutdownService уже вызовется через ContextClosedEvent
    }
}