package com.devlink.devlink.model;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStage {
    private String name;                    // "Аванс", "Первый этап", "Финальная оплата"
    private String description;             // Что должно быть сделано
    private Double amount;                  // Сумма этапа
    private Integer percentage;             // Процент от общей суммы

    @Builder.Default
    private Boolean isPaid = false;         // Оплачен ли этап

    @Builder.Default
    private Boolean isCompleted = false;    // Выполнен ли этап

    private LocalDateTime paidAt;           // Когда оплачен
    private LocalDateTime completedAt;      // Когда выполнен

    private String paymentProof;
    private String workProof;
}