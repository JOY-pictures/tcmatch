package com.tcmatch.tcmatch.model.dto;

import com.tcmatch.tcmatch.model.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO для хранения состояния Заказчика во время создания нового Заказа.
 * Сохраняется в UserSessionService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreationState implements Serializable {

    private Long customerChatId;

    // ID отклика, который заказчик принял
    private Long applicationId;

    // ID проекта, к которому относится отклик
    private Long projectId;

    // Выбранный тип оплаты
    private PaymentType paymentType;

    // Количество этапов (по умолчанию 1)
    private Integer milestoneCount = 1;

    // Шаг мастера, на котором находится заказчик (для удобства)
    private CreationStep currentStep;

    public OrderCreationState(Long customerChatId, Long applicationId, Long projectId) {
        this.customerChatId = customerChatId;
        this.applicationId = applicationId;
        this.projectId = projectId;
        this.currentStep = CreationStep.PAYMENT_TYPE_CHOICE; // Начинаем с выбора схемы
    }

    // Этапы Мастера
    public enum CreationStep {
        PAYMENT_TYPE_CHOICE, // Выбор: Полная/Поэтапная
        MILESTONE_COUNT_CHOICE, // Если Поэтапная, то выбор количества этапов
        CONFIRMATION, // Финальное подтверждение
        COMPLETED // Заказ создан
    }
}
