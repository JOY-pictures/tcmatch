package com.tcmatch.tcmatch.model.dto;

import lombok.Data;

@Data
public class ProjectCreationState {

    private Long chatId;
    private ProjectCreationStep currentStep;
    private boolean editing = false;

    // Данные проекта
    private String title;
    private String description;
    private Double budget;
    private Integer estimatedDays;
    private String requiredSkills;

    // Технические поля
    private Integer messageIdToDelete;

    public ProjectCreationState(Long chatId) {
        this.chatId = chatId;
        this.currentStep = ProjectCreationStep.TITLE;
    }

    public enum ProjectCreationStep {
        TITLE,
        DESCRIPTION,
        BUDGET,
        DEADLINE,
        SKILLS,
        CONFIRMATION
    }

    public void moveToNextStep() {
        this.currentStep = ProjectCreationStep.values()[this.currentStep.ordinal() + 1];
        this.editing = false;
    }

    public void moveToEditField(String field) {
        this.currentStep = ProjectCreationStep.valueOf(field.toUpperCase());
        this.editing = true;
    }

    public void finishEditing() {
        this.currentStep = ProjectCreationStep.CONFIRMATION;
        this.editing = false;
    }

    public boolean isCompleted() {
        return title != null && !title.trim().isEmpty() &&
                description != null && !description.trim().isEmpty() &&
                budget != null && budget > 0 &&
                estimatedDays != null && estimatedDays > 0 &&
                requiredSkills != null && !requiredSkills.trim().isEmpty();
    }
}
