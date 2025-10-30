package com.tcmatch.tcmatch.model;



import com.tcmatch.tcmatch.model.enums.UserRole;

import lombok.Data;



import java.time.LocalDateTime;



@Data

public class ProjectCreationState {

    private Long chatId;

    private UserRole.ProjectCreationStep currentStep = UserRole.ProjectCreationStep.TITLE;

    private String title;

    private String description;

    private Double budget;

    private Integer estimatedDays;

    private String requiredSkills;

    private LocalDateTime deadline;

    private LocalDateTime createdAt;



    public ProjectCreationState() {

        this.createdAt = LocalDateTime.now();

    }



    public boolean inStep(UserRole.ProjectCreationStep step) {

        return this.currentStep == step;

    }



    public void moveToNextStep() {

        this.currentStep = switch (this.currentStep) {

            case TITLE -> UserRole.ProjectCreationStep.DESCRIPTION;

            case DESCRIPTION -> UserRole.ProjectCreationStep.BUDGET;

            case BUDGET -> UserRole.ProjectCreationStep.DEADLINE;

            case DEADLINE -> UserRole.ProjectCreationStep.SKILLS;

            case SKILLS -> UserRole.ProjectCreationStep.CONFIRMATION;

            case CONFIRMATION -> UserRole.ProjectCreationStep.CONFIRMATION;

        };

    }



    public boolean isCompleted() {

        return title != null && description != null && budget != null && estimatedDays != null && requiredSkills != null && deadline != null;

    }

}
