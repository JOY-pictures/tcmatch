package com.tcmatch.tcmatch.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcmatch.tcmatch.model.Transaction;
import com.tcmatch.tcmatch.payment.yoomoney.dto.YooMoneyNotification;
import com.tcmatch.tcmatch.repository.TransactionRepository;
import com.tcmatch.tcmatch.service.BalancePaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payment")
@Slf4j
@RequiredArgsConstructor
public class YYNotificationController {

    private final TransactionRepository transactionRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    private final BalancePaymentService paymentService;

    @GetMapping("/info")
    public Map<String, String> getInfo(HttpServletRequest request) {
        Map<String, String> info = new HashMap<>();
        info.put("baseUrl", baseUrl);
        info.put("serverPort", String.valueOf(request.getServerPort()));
        info.put("scheme", request.getScheme());
        info.put("secure", String.valueOf(request.isSecure()));
        info.put("requestURL", request.getRequestURL().toString());
        info.put("x-forwarded-proto", request.getHeader("X-Forwarded-Proto"));
        return info;
    }

    // Тестовый endpoint для проверки POST
    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody String body, HttpServletRequest request) {
        log.info("📥 Echo received: {}", body);
        log.info("Headers:");
        request.getHeaderNames().asIterator().forEachRemaining(header ->
                log.info("  {}: {}", header, request.getHeader(header)));

        return ResponseEntity.ok(Map.of(
                "status", "received",
                "body", body,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Обрабатывает POST-уведомления (вебхуки) от ЮKassa.
     */
    @PostMapping("/notify")
    public ResponseEntity<Void> handleNotification(@RequestBody String rawBody) {

        try {
//            log.info("📥 Получено уведомление от ЮKassa:");
//            log.info("Raw body: {}", rawBody);
//
//            // Парсим JSON вручную для отладки
            ObjectMapper mapper = new ObjectMapper();
//            JsonNode rootNode = mapper.readTree(rawBody);
//
//            log.info("Parsed JSON structure:");
//            log.info("  - type: {}", rootNode.get("type"));
//            log.info("  - event: {}", rootNode.get("event"));
//            log.info("  - object: {}", rootNode.get("object"));

            // Пробуем парсить в DTO
            YooMoneyNotification notification = mapper.readValue(rawBody, YooMoneyNotification.class);

            // 1. ПРОВЕРКА ПОДПИСИ (В ПРОДАКШЕНЕ - ОБЯЗАТЕЛЬНА!)
            // В реальной системе здесь должна быть проверка HMAC-подписи
            // (хеша), чтобы убедиться, что запрос пришел от ЮKassa.
            // Сейчас мы ее пропускаем, но это критически важная мера безопасности.

            // 2. Базовая проверка данных
            if (notification == null || notification.getObject() == null) {
                log.warn("Получено пустое уведомление от ЮKassa.");
                return ResponseEntity.badRequest().build();
            }

            String paymentId = notification.getObject().getId();
            String status = notification.getObject().getStatus();

            if (paymentId == null || status == null) {
                log.warn("Уведомление не содержит ID платежа или статуса.");
                return ResponseEntity.badRequest().build();
            }

            // 3. Передача данных в сервис для бизнес-логики (активации/отмены)
            try {
                paymentService.handlePaymentNotification(paymentId, status);
            } catch (Exception e) {
                // Если произошла ошибка внутри нашего сервиса (например, ошибка БД),
                // возвращаем статус 500, чтобы ЮKassa попыталась отправить уведомление повторно.
                log.error("Критическая ошибка при обработке уведомления {}: {}", paymentId, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // 4. Всегда возвращаем 200 OK (или 204 No Content)
            // Это сигнализирует ЮKassa, что мы успешно приняли уведомление, и предотвращает повторную отправку.
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке уведомления: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // 🔥 Новый endpoint для ручной проверки статуса платежа
    @PostMapping("/check-status")
    public ResponseEntity<String> checkPaymentStatus(@RequestParam String paymentId) {
        try {
            log.info("🔍 Ручная проверка статуса платежа: {}", paymentId);

            // 1. Проверяем в нашей БД
            Optional<Transaction> txOpt = transactionRepository.findByPaymentId(paymentId);

            if (txOpt.isPresent()) {
                Transaction tx = txOpt.get();
                return ResponseEntity.ok(String.format(
                        "Статус в БД: %s, пользователь: %d, тариф: %s",
                        tx.getStatus(), tx.getChatId(), tx.getTier()
                ));
            }

            // 2. Если нет в БД, можно запросить у ЮKassa API
            return ResponseEntity.ok("Платеж не найден в БД");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }
}