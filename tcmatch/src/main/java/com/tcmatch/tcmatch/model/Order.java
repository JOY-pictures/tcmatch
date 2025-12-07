package com.tcmatch.tcmatch.model;


import com.tcmatch.tcmatch.model.enums.OrderStatus;
import com.tcmatch.tcmatch.model.enums.PaymentType;
import com.tcmatch.tcmatch.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // Важно: "order" часто является зарезервированным словом в SQL
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    /**
     * Статус заморозки средств для Escrow.
     * PENDING: Ожидает заморозки средств.
     * FROZEN: Средства заморожены (заказ в работе).
     * RELEASED: Средства выплачены исполнителю.
     * REFUNDED: Средства возвращены заказчику.
     */
    public enum EscrowStatus {
        PENDING,
        FROZEN,
        RELEASED,
        REFUNDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ссылка на проект
    @Column(nullable = false)
    private Long projectId;

    // Ссылка на отклик, который был принят
    @Column(nullable = false, unique = true)
    private Long applicationId;

    // ID заказчика (chatId или userId, в зависимости от твоей структуры)
    @Column(nullable = false)
    private Long customerChatId;

    // ID исполнителя (chatId или userId)
    @Column(nullable = false)
    private Long freelancerChatId;

    // Бюджет, который исполнитель предложил в отклике
    @Column(nullable = false)
    private double totalBudget;

    // Срок, который исполнитель предложил
    @Column(nullable = false)
    private Integer estimatedDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    // Количество этапов (если paymentType = MILESTONES).
    // Если FULL, то это 1.
    @Column(nullable = false)
    private Integer milestoneCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 🔥 НОВОЕ ПОЛЕ: Статус заморозки средств
    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", nullable = false)
    @Builder.Default
    private EscrowStatus escrowStatus = EscrowStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
