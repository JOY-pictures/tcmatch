package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.dto.ApplicationCreationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ApplicationCreationService {

    private final UserSessionService userSessionService;

    private final Map<Long, ApplicationCreationState> userCreationState = new ConcurrentHashMap<>();

    public ApplicationCreationService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    public void startApplicationCreation(Long chatId, Long projectId) {
        ApplicationCreationState state = new ApplicationCreationState(chatId, projectId);

        // 🔥 СОХРАНЯЕМ СОСТОЯНИЕ В USERSESSIONSERVICE
        userSessionService.setApplicationCreationState(chatId, state);
        userSessionService.setCurrentHandler(chatId, "application");
        userSessionService.setCurrentAction(chatId, "application", "creating");

        log.info("🚀 Started application creation for user {} on project {}", chatId, projectId);
    }

    public ApplicationCreationState getCurrentState(Long chatId) {
        // 🔥 ПОЛУЧАЕМ СОСТОЯНИЕ ИЗ USERSESSIONSERVICE
        return userSessionService.getApplicationCreationState(chatId);
    }

    public ApplicationCreationState.ApplicationCreationStep getCurrentStep(Long chatId) {
        ApplicationCreationState state = userCreationState.get(chatId);
        return state != null ? state.getCurrentStep() : null;
    }

    public void updateCurrentState(Long chatId, ApplicationCreationState state) {
        // 🔥 ОБНОВЛЯЕМ СОСТОЯНИЕ В USERSESSIONSERVICE
        userSessionService.setApplicationCreationState(chatId, state);
    }

    public void cancelCreation(Long chatId) {
        // 🔥 ОЧИЩАЕМ СОСТОЯНИЕ В USERSESSIONSERVICE
        userSessionService.clearApplicationCreationState(chatId);
        userSessionService.clearHandlerState(chatId, "application");
        log.info("❌ Cancelled application creation for user: {}", chatId);
    }

    public void completeCreation(Long chatId) {
        userCreationState.remove(chatId);
        log.info("✅ Completed application creation for user: {}", chatId);
    }

    public boolean isCreatingApplication(Long chatId) {
        // 🔥 ПРОВЕРЯЕМ ЧЕРЕЗ USERSESSIONSERVICE
        String currentHandler = userSessionService.getCurrentHandler(chatId);
        return "application".equals(currentHandler) &&
                userSessionService.getApplicationCreationState(chatId) != null;
    }
}
