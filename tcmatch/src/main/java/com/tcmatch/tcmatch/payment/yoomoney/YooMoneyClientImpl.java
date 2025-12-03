package com.tcmatch.tcmatch.payment.yoomoney;

import com.tcmatch.tcmatch.payment.yoomoney.dto.YooMoneyPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class YooMoneyClientImpl implements YooMoneyClient {

    private final RestTemplate restTemplate;

    @Value("${yoomoney.shopId}")
    private String shopId;

    @Value("${yoomoney.secretKey}")
    private String secretKey;

    @Value("${yoomoney.returnUrl}")
    private String returnUrl;

    // ✅ КРИТИЧНО: Используем @Qualifier для получения именованного бина
    public YooMoneyClientImpl(@Qualifier("yooMoneyRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public YooMoneyPaymentResponse createPayment(Double amount, String description, UUID idempotenceKey) {

        // 🔥 Убедитесь, что здесь Double, а не BigDecimal
        String formattedAmount = String.format(Locale.ROOT, "%.2f", amount);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", Map.of(
                "value", formattedAmount,
                "currency", "RUB"
        ));
        requestBody.put("capture", true);
        requestBody.put("description", description);
        requestBody.put("confirmation", Map.of("type", "redirect", "return_url", returnUrl));

        HttpHeaders headers = new HttpHeaders();

        headers.set("Idempotence-Key", idempotenceKey.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("📤 Отправка запроса в ЮKassa: amount={}, description={}", formattedAmount, description);

            ResponseEntity<YooMoneyPaymentResponse> response = restTemplate.exchange(
                    "/payments",
                    HttpMethod.POST,
                    entity,
                    YooMoneyPaymentResponse.class
            );

            log.info("📥 Получен ответ от ЮKassa: status={}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                YooMoneyPaymentResponse responseBody = response.getBody();

                // 🔥 ДЕТАЛЬНОЕ ЛОГИРОВАНИЕ ОТВЕТА
                log.info("✅ Ответ от ЮKassa:");
                log.info("  - ID: {}", responseBody.getId());
                log.info("  - Status: {}", responseBody.getStatus());
                log.info("  - Confirmation: {}", responseBody.getConfirmation());

                if (responseBody.getConfirmation() != null) {
                    log.info("  - Confirmation Type: {}", responseBody.getConfirmation().getType());
                    log.info("  - Confirmation URL: {}", responseBody.getConfirmation().getConfirmationUrl());
                } else {
                    log.warn("⚠️ Confirmation is NULL в ответе!");
                }

                return responseBody;
            } else {
                log.error("❌ YooKassa API Error. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Ошибка API ЮKassa: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ YooKassa connection error: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось установить связь с платежным шлюзом: " + e.getMessage());
        }
    }
}