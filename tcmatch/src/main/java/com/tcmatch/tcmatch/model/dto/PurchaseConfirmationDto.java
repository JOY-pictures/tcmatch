package com.tcmatch.tcmatch.model.dto;

import com.tcmatch.tcmatch.model.enums.PurchaseType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO для запроса подтверждения покупки
 */
@Data
@Builder
public class PurchaseConfirmationDto {
    private Long chatId;
    private PurchaseType purchaseType;
    private BigDecimal amount;
    private String targetId; // ID подписки, заказа и т.д.
    private String description;
    private Integer messageId; // ID сообщения для обновления

    // 🔥 НОВОЕ: Команда для выполнения после успешной оплаты
    private String successCallback; // Формат: "actionType:action:parameter"

    // 🔥 НОВОЕ: Команда для выполнения при отмене
    private String cancelCallback; // Формат: "actionType:action:parameter"
}