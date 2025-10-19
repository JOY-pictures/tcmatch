package com.devlink.devlink.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private Double budget;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProjectStatus statu = ProjectStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id")
    private User freelancer;

    private LocalDateTime deadline;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private String requiredSkills;
    private Integer estimatedDays;

    @Builder.Default
    private Integer viewsCount = 0;

    @Builder.Default
    private Integer applicationsCount = 0;
}
