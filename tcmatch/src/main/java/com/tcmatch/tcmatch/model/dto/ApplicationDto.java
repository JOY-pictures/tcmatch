package com.tcmatch.tcmatch.model.dto;

import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationDto {
    private Long id;
    private String coverLetter;
    private Double proposedBudget;
    private Integer proposedDays;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private String customerComment;
    private UserRole.ApplicationStatus status;

    // 🔥 ВМЕСТО ССЫЛОК - ХРАНИМ ТОЛЬКО ID
    private Long projectId;
    private Long freelancerChatId;

    // 🔥 ОПЦИОНАЛЬНО: ДАННЫЕ ПРОЕКТА И ФРИЛАНСЕРА (ЕСЛИ НУЖНЫ)
    private ProjectDto project;
    private UserDto freelancer;

    public static ApplicationDto fromEntity(com.tcmatch.tcmatch.model.Application entity, ProjectDto project, UserDto freelancer) {
        if (entity == null) return null;

        ApplicationDto dto = new ApplicationDto();
        dto.setId(entity.getId());
        dto.setCoverLetter(entity.getCoverLetter());
        dto.setProposedBudget(entity.getProposedBudget());
        dto.setProposedDays(entity.getProposedDays());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setCustomerComment(entity.getCustomerComment());
        dto.setStatus(entity.getStatus());

        // 🔥 ЗАМЕНИТЕ НА РЕАЛЬНЫЕ ПОЛЯ ИЗ ВАШЕЙ СУЩНОСТИ Application
        dto.setProjectId(entity.getProjectId() != null ? entity.getProjectId() : null);
        dto.setFreelancerChatId(entity.getFreelancerChatId() != null ? entity.getFreelancerChatId() : null);

        dto.setProject(project);
        dto.setFreelancer(freelancer);

        return dto;
    }

    public static ApplicationDto fromEntity(com.tcmatch.tcmatch.model.Application entity) {
        if (entity == null) return null;

        ApplicationDto dto = new ApplicationDto();
        dto.setId(entity.getId());
        dto.setCoverLetter(entity.getCoverLetter());
        dto.setProposedBudget(entity.getProposedBudget());
        dto.setProposedDays(entity.getProposedDays());
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setCustomerComment(entity.getCustomerComment());
        dto.setStatus(entity.getStatus());

        // 🔥 ЗАМЕНИТЕ НА РЕАЛЬНЫЕ ПОЛЯ
        dto.setProjectId(entity.getProjectId() != null ? entity.getProjectId() : null);
        dto.setFreelancerChatId(entity.getFreelancerChatId() != null ? entity.getFreelancerChatId() : null);

        return dto;
    }
}