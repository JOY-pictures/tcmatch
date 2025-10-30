package com.tcmatch.tcmatch.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationCreationState {
    private Long chatId;
    private Long projectId;
    private ApplicationCreationStep currentStep = ApplicationCreationStep.DESCRIPTION;
    private String coverLetter;
    private Double proposedBudget;
    private Integer proposedDays;
    private LocalDateTime createdAt;
    private boolean isEditing = false; // 🔥 ФЛАГ РЕДАКТИРОВАНИЯ

    public ApplicationCreationState(Long chatId, Long projectId) {
        this.chatId = chatId;
        this.projectId = projectId;
        this.createdAt = LocalDateTime.now();
    }

    public enum ApplicationCreationStep {
        DESCRIPTION,
        BUDGET,
        DEADLINE,
        CONFIRMATION
    }

    public void moveToNextStep() {
        this.currentStep = switch (this.currentStep) {
            case DESCRIPTION -> ApplicationCreationStep.BUDGET;
            case BUDGET -> ApplicationCreationStep.DEADLINE;
            case DEADLINE -> ApplicationCreationStep.CONFIRMATION;
            case CONFIRMATION -> ApplicationCreationStep.CONFIRMATION;
        };
    }

    public void moveToPreviousStep() {
        this.currentStep = switch (this.currentStep) {
            case DESCRIPTION -> ApplicationCreationStep.DESCRIPTION;
            case BUDGET -> ApplicationCreationStep.DESCRIPTION;
            case DEADLINE -> ApplicationCreationStep.BUDGET;
            case CONFIRMATION -> ApplicationCreationStep.DEADLINE;
        };
    }

    // 🔥 МЕТОД ДЛЯ ПЕРЕХОДА К РЕДАКТИРОВАНИЮ ПОЛЯ
    public void moveToEditField(String field) {
        this.isEditing = true;
        this.currentStep = switch (field) {
            case "description" -> ApplicationCreationStep.DESCRIPTION;
            case "budget" -> ApplicationCreationStep.BUDGET;
            case "deadline" -> ApplicationCreationStep.DEADLINE;
            default -> this.currentStep;
        };
    }

    // 🔥 МЕТОД ДЛЯ ЗАВЕРШЕНИЯ РЕДАКТИРОВАНИЯ
    public void finishEditing() {
        this.isEditing = false;
        this.currentStep = ApplicationCreationStep.CONFIRMATION;
    }

    public boolean isCompleted() {
        return coverLetter != null && proposedBudget != null && proposedDays != null;
    }

    // 🔥 ПРОВЕРКА - МОЖНО ЛИ ПЕРЕЙТИ К ПОДТВЕРЖДЕНИЮ
    public boolean canProceedToConfirmation() {
        return coverLetter != null && proposedBudget != null && proposedDays != null;
    }
}