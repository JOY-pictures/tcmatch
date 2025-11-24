package com.tcmatch.tcmatch.service;


import com.tcmatch.tcmatch.model.dto.ProjectCreationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectCreationService {

    private final UserSessionService userSessionService;

    public ProjectCreationService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    public void startProjectCreation(Long chatId) {

        ProjectCreationState state = new ProjectCreationState(chatId);

        userSessionService.setProjectCreationState(chatId, state);
        userSessionService.setCurrentCommand(chatId, "project");
        userSessionService.setCurrentAction(chatId, "project", "create");

        log.info("🚀 Started project creation for user {}", chatId);
    }

    public ProjectCreationState getCurrentState(Long chatId) {
        return userSessionService.getProjectCreationState(chatId);
    }

    public void updateCurrentState(Long chatId, ProjectCreationState state) {
        userSessionService.setProjectCreationState(chatId, state);
    }

    public void cancelCreation(Long chatId) {
        userSessionService.clearProjectCreationState(chatId);
        userSessionService.clearHandlerState(chatId, "project");
        log.info("❌ Cancelled project creation for user: {}", chatId);
    }

    public void completeCreation(Long chatId) {
        userSessionService.clearProjectCreationState(chatId);
        userSessionService.clearHandlerState(chatId, "project_creation");
        log.info("✅ Completed project creation for user: {}", chatId);
    }

    public boolean isCreatingProject(Long chatId) {
        String currentHandler = userSessionService.getCurrentCommand(chatId);
        String currentAction = userSessionService.getCurrentAction(chatId);
        return "project".equals(currentHandler) &&
                "create".equals(currentAction) &&
                userSessionService.getProjectCreationState(chatId) != null;
    }

    public void processInputAndValidate(ProjectCreationState state, String text) {
        switch (state.getCurrentStep()) {
            case TITLE:
                handleTitleInput(state, text);
                break;
            case DESCRIPTION:
                handleDescriptionInput(state, text);
                break;
            case BUDGET:
                handleBudgetInput(state, text);
                break;
            case DEADLINE:
                handleDeadlineInput(state, text);
                break;
            case SKILLS:
                handleSkillsInput(state, text);
                break;
            default:
                throw new IllegalStateException("Шаг " + state.getCurrentStep() + " не ожидает текстового ввода.");
        }
        userSessionService.setProjectCreationState(state.getChatId(), state);
    }

    private void handleTitleInput(ProjectCreationState state, String text) {
        if (text.length() < 5) {
            throw new IllegalArgumentException("Название проекта должно содержать минимум 5 символов.");
        }
        if (text.length() > 100) {
            throw new IllegalArgumentException("Название проекта слишком длинное. Максимум 100 символов.");
        }
        state.setTitle(text.trim());
    }

    private void handleDescriptionInput(ProjectCreationState state, String text) {
        if (text.length() < 20) {
            throw new IllegalArgumentException("Описание проекта должно содержать минимум 20 символов.");
        }
        if (text.length() > 3000) {
            throw new IllegalArgumentException("Описание проекта слишком длинное. Максимум 2000 символов.");
        }
        state.setDescription(text.trim());
    }

    private void handleBudgetInput(ProjectCreationState state, String text) {
        try {
            double budget = Double.parseDouble(text.replace(",", ".").trim());
            if (budget < 1000) {
                throw new IllegalArgumentException("Бюджет проекта должен быть не менее 1000 руб.");
            }
            if (budget > 1_000_000) {
                throw new IllegalArgumentException("Бюджет проекта не может превышать 1 000 000 руб.");
            }
            state.setBudget(budget);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Введите бюджет как число (например, 50000 или 50000.50).");
        }
    }

    private void handleDeadlineInput(ProjectCreationState state, String text) {
        try {
            int days = Integer.parseInt(text.trim());
            if (days < 1) {
                throw new IllegalArgumentException("Срок выполнения должен быть не менее 1 дня.");
            }
            if (days > 365) {
                throw new IllegalArgumentException("Срок выполнения не может превышать 365 дней.");
            }
            state.setEstimatedDays(days);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Введите срок выполнения как целое число дней (например, 7).");
        }
    }

    private void handleSkillsInput(ProjectCreationState state, String text) {
        if (text.length() < 3) {
            throw new IllegalArgumentException("Укажите хотя бы один навык (минимум 3 символа).");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("Список навыков слишком длинный. Максимум 500 символов.");
        }
        state.setRequiredSkills(text.trim());
    }
}
