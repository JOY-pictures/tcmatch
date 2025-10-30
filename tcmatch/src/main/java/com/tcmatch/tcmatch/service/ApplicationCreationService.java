package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ApplicationCreationService {
    private final Map<Long, ApplicationCreationState> userCreationState = new ConcurrentHashMap<>();

    public void startApplicationCreation(Long chatId, Long projectId) {
        ApplicationCreationState state = new ApplicationCreationState(chatId, projectId);
        userCreationState.put(chatId, state);
        log.info("🚀 Started application creation for user {} on project {}", chatId, projectId);
    }

    public ApplicationCreationState getCurrentState(Long chatId) {
        return userCreationState.get(chatId);
    }

    public ApplicationCreationState.ApplicationCreationStep getCurrentStep(Long chatId) {
        ApplicationCreationState state = userCreationState.get(chatId);
        return state != null ? state.getCurrentStep() : null;
    }

    public void updateCurrentState(Long chatId, ApplicationCreationState state) {
        userCreationState.put(chatId, state);
    }

    public void cancelCreation(Long chatId) {
        userCreationState.remove(chatId);
        log.info("❌ Cancelled application creation for user: {}", chatId);
    }

    public void completeCreation(Long chatId) {
        userCreationState.remove(chatId);
        log.info("✅ Completed application creation for user: {}", chatId);
    }

    public boolean isCreatingApplication(Long chatId) {
        return userCreationState.containsKey(chatId);
    }
}
