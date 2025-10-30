package com.tcmatch.tcmatch.model;


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
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToOne
    @JoinColumn(name = "application_if", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    private String title;
    private String description;
    private Double totalBudget;
    private Integer estimatedDays;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole.OrderStatus status = UserRole.OrderStatus.CREATED;

    // Система правок
    @Builder.Default
    private Integer revisionCount = 0;

    @Builder.Default
    private Integer clarificationCount = 0;

    @Builder.Default
    private Integer maxRevisions = 3;

    @Builder.Default
    private Integer maxClarifications = 5;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime deadline;

    // Поэтапная оплата
    @ElementCollection
    @Builder.Default
    private List<PaymentStage> paymentStages = new ArrayList<>();

    //поле для текущих комментариев к правкам
    private String currentRevisionNotes;

    //история всех правок
    @ElementCollection
    @CollectionTable(name = "order_revision_history", joinColumns = @JoinColumn(name = "order_id"))
    @Builder.Default
    private List<RevisionNote> revisionHistory = new ArrayList<>();

    // Дополнительные поля
    private String customerRequirements;
    private String workResult;

    private String customerFeedback; // Отзыв заказчика
    private String freelancerFeedback; // Отзыв исполнителя

    @Builder.Default
    private Double customerRating = 0.0;

    @Builder.Default
    private Double freelancerRating = 0.0;
}
