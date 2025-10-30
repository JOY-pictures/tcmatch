package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.UserRole;
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
    private UserRole.ProjectStatus status = UserRole.ProjectStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id")
    private User freelancer;

    private LocalDateTime deadline = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime completedAt = LocalDateTime.now();

    private String requiredSkills;
    private Integer estimatedDays;

    @Builder.Default
    private Integer viewsCount = 0;

    @Builder.Default
    private Integer applicationsCount = 0;
}
