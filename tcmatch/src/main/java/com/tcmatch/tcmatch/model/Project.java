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

    @Column(length = 3300)
    private String description;

    private Double budget;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole.ProjectStatus status = UserRole.ProjectStatus.OPEN;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    // 🔥 ЗАМЕНЯЕМ ССЫЛКУ НА User НА ID
    @Column(name = "customer_chat_id")
    private Long customerChatId;

    // 🔥 ЗАМЕНЯЕМ ССЫЛКУ НА User (исполнителя) НА ID
    @Column(name = "freelancer_chat_id")
    private Long freelancerChatId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private String requiredSkills;
    private Integer estimatedDays;

    @Builder.Default
    private Integer viewsCount = 0;

    @Builder.Default
    private Integer applicationsCount = 0;
}
