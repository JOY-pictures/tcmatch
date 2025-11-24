package com.tcmatch.tcmatch.model.dto;

import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectDto {
    private Long id;
    private String title;
    private String description;
    private Double budget;
    private Integer estimatedDays;
    private LocalDateTime deadline;
    private String requiredSkills;
    private Integer viewsCount;
    private Integer applicationsCount;
    private UserRole.ProjectStatus status;
    private LocalDateTime createdAt;

    // 🔥 ВМЕСТО ССЫЛОК НА User - ХРАНИМ ТОЛЬКО ID И ОСНОВНЫЕ ДАННЫЕ
    private Long customerChatId;
    private String customerUserName;
    private String customerDisplayName;
    private Double customerRating;

    private Long freelancerChatId;

    public static ProjectDto fromEntity(com.tcmatch.tcmatch.model.Project entity, UserDto customer) {
        if (entity == null) return null;

        ProjectDto dto = new ProjectDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setBudget(entity.getBudget());
        dto.setEstimatedDays(entity.getEstimatedDays());
        dto.setDeadline(entity.getDeadline());
        dto.setRequiredSkills(entity.getRequiredSkills());
        dto.setViewsCount(entity.getViewsCount());
        dto.setApplicationsCount(entity.getApplicationsCount());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());

        // 🔥 ЕСЛИ В СУЩНОСТИ Project ЕСТЬ customer ПОЛЕ - ЗАМЕНИТЕ ЕГО НА customerId
        // Сейчас используем заглушки - замените на реальные поля из вашей сущности Project
        dto.setCustomerChatId(entity.getCustomerChatId() != null ? entity.getCustomerChatId() : null);

        if (customer != null) {
            dto.setCustomerUserName(customer.getUserName());
            dto.setCustomerDisplayName(customer.getDisplayName());
            dto.setCustomerRating(customer.getProfessionalRating());
        }

        return dto;
    }
}
