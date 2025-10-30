package com.tcmatch.tcmatch.service;


import com.tcmatch.tcmatch.model.ProjectCreationState;
import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ProjectCreationService {

    private final Map<Long, ProjectCreationState> userCreationState = new ConcurrentHashMap<>();

    public void startProjectCreation(Long chatId) {
        ProjectCreationState state = new ProjectCreationState();
        state.setChatId(chatId);
        state.setCurrentStep(UserRole.ProjectCreationStep.TITLE);
        userCreationState.put(chatId, state);
        log.info("🚀 Started project creation for user: {}", chatId);
    }

    public ProjectCreationState getCurrentState(Long chatId) {
        return userCreationState.get(chatId);
    }

    public UserRole.ProjectCreationStep getCurrentStep(Long chatId) {
        ProjectCreationState state = userCreationState.get(chatId);
        return state != null ? state.getCurrentStep() : null;
    }

    public void cancelCreation(Long chatId) {
        userCreationState.remove(chatId);
        log.info("❌ Cancelled project creation for user: {}", chatId);
    }

    public boolean isCreatingProject(Long chatId) {
        return userCreationState.containsKey(chatId);
    }
}
