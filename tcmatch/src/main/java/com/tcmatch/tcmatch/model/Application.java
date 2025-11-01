package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(length = 3200)
    private String coverLetter;

    private Double proposedBudget; // Предложенный бюджет (может отличаться от проекта)
    private Integer proposedDays; // Предложенные сроки

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole.ApplicationStatus status = UserRole.ApplicationStatus.PENDING;

    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;
    private String customerComment;
}
