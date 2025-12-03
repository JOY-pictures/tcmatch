package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.exceptions.DescriptionTooLongException;
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
        userSessionService.setCurrentCommand(chatId, "application");
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
        userSessionService.clearCurrentCommand(chatId);
        log.info("❌ Cancelled application creation for user: {}", chatId);
    }

    public void completeCreation(Long chatId) {
        userCreationState.remove(chatId);
        log.info("✅ Completed application creation for user: {}", chatId);
    }

    public boolean isCreatingApplication(Long chatId) {
        // 🔥 ПРОВЕРЯЕМ ЧЕРЕЗ USERSESSIONSERVICE
        String currentHandler = userSessionService.getCurrentCommand(chatId);
        return "application".equals(currentHandler) &&
                userSessionService.getApplicationCreationState(chatId) != null;
    }

    // 🔥 Новый метод: Валидация и сохранение данных, но НЕ переход на следующий шаг
    public void processInputAndValidate(ApplicationCreationState state, String text) {
        switch (state.getCurrentStep()) {
            case DESCRIPTION:
                handleDescriptionInput(state, text);
                break;
            case BUDGET:
                handleBudgetInput(state, text);
                break;
            case DEADLINE:
                handleDeadlineInput(state, text);
                break;
            default:
                throw new IllegalStateException("Шаг " + state.getCurrentStep() + " не ожидает текстового ввода.");
        }
        // Сохраняем состояние в сессии после валидации
        userSessionService.setApplicationCreationState(state.getChatId(), state);
    }

    private void handleDescriptionInput(ApplicationCreationState state, String text) {
        if (text.length() < 10) {
            throw new IllegalArgumentException("Описание должно содержать минимум 10 символов.");
        }
        if (text.length() > 3200) {
            // 🔥 Выбрасываем кастомное исключение, чтобы TCMatchBot не удалил сообщение
            throw new DescriptionTooLongException("Слишком длинное описание. Максимум 3200 символов. Пожалуйста, сократите (на %d)и отправьте повторно.".formatted(text.length() - 3200));
        }
        state.setCoverLetter(text);
    }

    private void handleBudgetInput(ApplicationCreationState state, String text) {
        try {
            double budget = Double.parseDouble(text.replace(",", ".").trim());
            if (budget <= 0 || budget > 1_000_000) {
                throw new IllegalArgumentException("Бюджет должен быть положительным числом (до 1 000 000 руб).");
            }
            state.setProposedBudget(budget);
        } catch (NumberFormatException e) {
            // При NumberFormatException бросаем его, чтобы TCMatchBot удалил сообщение
            throw new NumberFormatException("Введите бюджет как число (например, 50000 или 50000.00).");
        }
    }

    private void handleDeadlineInput(ApplicationCreationState state, String text) {
        try {
            int days = Integer.parseInt(text.trim());
            if (days <= 0 || days > 365) {
                throw new IllegalArgumentException("Срок должен быть целым числом дней (от 1 до 365).");
            }
            state.setProposedDays(days);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Введите срок как целое число дней (например, 7).");
        }
    }


}
