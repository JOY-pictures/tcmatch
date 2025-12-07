package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.events.NewVerificationRequestEvent;
import com.tcmatch.tcmatch.events.VerificationStatusChangedEvent;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.model.enums.VerificationType;
import com.tcmatch.tcmatch.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationRequestRepository verificationRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // Простая проверка GitHub URL
    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile("^https://github\\.com/[a-zA-Z0-9](?:[a-zA-Z0-9]|-(?=[a-zA-Z0-9])){0,38}$");

    /**
     * 🔥 Создать заявку на верификацию GitHub
     */
    @Transactional
    public VerificationRequest createGitHubVerificationRequest(Long userChatId, String githubUrl) {
        // 1. Получаем пользователя
        UserDto user = userService.getUserDtoByChatId(userChatId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Простая валидация URL
        if (!isValidGitHubUrl(githubUrl)) {
            throw new IllegalArgumentException("Неверный формат GitHub URL. Пример: https://github.com/username");
        }

        // 3. Проверяем, нет ли уже активной заявки
        Optional<VerificationRequest> existingPending = verificationRepository
                .findByUserChatIdAndTypeAndStatus(userChatId, VerificationType.GITHUB, VerificationStatus.PENDING);

        if (existingPending.isPresent()) {
            throw new IllegalStateException("У вас уже есть активная заявка на рассмотрении");
        }

        // 4. Проверяем, не верифицирован ли уже
        Optional<VerificationRequest> existingApproved = verificationRepository
                .findByUserChatIdAndTypeAndStatus(userChatId, VerificationType.GITHUB, VerificationStatus.APPROVED);

        if (existingApproved.isPresent()) {
            throw new IllegalStateException("Ваш GitHub уже верифицирован");
        }

        // 5. Создаем заявку
        VerificationRequest request = VerificationRequest.builder()
                .userChatId(userChatId)
                .userName(user.getUserName()) // Сохраняем для удобства админов
                .type(VerificationType.GITHUB)
                .providedData(githubUrl.trim())
                .status(VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        VerificationRequest savedRequest = verificationRepository.save(request);

        // 6. Отправляем событие для уведомления админов
        eventPublisher.publishEvent(new NewVerificationRequestEvent(savedRequest));

        log.info("✅ Создана заявка на верификацию GitHub #{} для пользователя {}",
                savedRequest.getId(), userChatId);

        return savedRequest;
    }

    /**
     * 🔥 Проверить валидность GitHub URL
     */
    private boolean isValidGitHubUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmed = url.trim();

        // Простая проверка
        return GITHUB_URL_PATTERN.matcher(trimmed).matches() &&
                trimmed.length() < 100 &&
                !trimmed.contains(" ");
    }

    /**
     * 🔥 Получить статус верификации GitHub для пользователя
     */
    public VerificationStatus getGitHubVerificationStatus(Long userChatId) {
        // Сначала ищем одобренную
        Optional<VerificationRequest> approved = verificationRepository
                .findByUserChatIdAndTypeAndStatus(userChatId, VerificationType.GITHUB, VerificationStatus.APPROVED);

        if (approved.isPresent()) {
            return VerificationStatus.APPROVED;
        }

        // Потом ищем на рассмотрении
        Optional<VerificationRequest> pending = verificationRepository
                .findByUserChatIdAndTypeAndStatus(userChatId, VerificationType.GITHUB, VerificationStatus.PENDING);

        if (pending.isPresent()) {
            return VerificationStatus.PENDING;
        }

        // Потом ищем отклоненную
        Optional<VerificationRequest> rejected = verificationRepository
                .findByUserChatIdAndTypeAndStatus(userChatId, VerificationType.GITHUB, VerificationStatus.REJECTED);

        if (rejected.isPresent()) {
            return VerificationStatus.REJECTED;
        }

        // Ничего не нашли
        return null;
    }

    /**
     * 🔥 Получить текущую заявку (если есть)
     */
    public Optional<VerificationRequest> getCurrentGitHubVerificationRequest(Long userChatId) {
        // Сначала активную
        Optional<VerificationRequest> pending = verificationRepository
                .findByUserChatIdAndTypeAndStatus(userChatId, VerificationType.GITHUB, VerificationStatus.PENDING);

        if (pending.isPresent()) {
            return pending;
        }

        // Или последнюю в истории
        return verificationRepository.findTopByUserChatIdAndTypeOrderByCreatedAtDesc(userChatId, VerificationType.GITHUB);
    }

    /**
     * 🔥 Получить все заявки на рассмотрении (для админов)
     */
    public List<VerificationRequest> getPendingVerificationRequests() {
        return verificationRepository.findByStatusOrderByCreatedAtDesc(VerificationStatus.PENDING);
    }

    /**
     * 🔥 Получить заявки GitHub на рассмотрении
     */
    public List<VerificationRequest> getPendingGitHubRequests() {
        return verificationRepository.findByTypeAndStatusOrderByCreatedAtDesc(
                VerificationType.GITHUB,
                VerificationStatus.PENDING);
    }

    /**
     * 🔥 Одобрить заявку
     */
    @Transactional
    public void approveVerification(Long requestId, Long adminChatId) {
        VerificationRequest request = verificationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (request.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("Заявка уже обработана");
        }

        // Обновляем статус заявки
        request.setStatus(VerificationStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminChatId);

        // Обновляем пользователя
        UserDto user = userService.getUserDtoByChatId(request.getUserChatId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (request.getType() == VerificationType.GITHUB) {
            // Обновляем поле GitHub URL у пользователя
            userService.updateUserGitHubUrl(request.getUserChatId(), request.getProvidedData());
            userService.markUserAsVerified(request.getUserChatId());
        }

        verificationRepository.save(request);

        // Отправляем событие для уведомления пользователя
        eventPublisher.publishEvent(new VerificationStatusChangedEvent(request, adminChatId));

        log.info("✅ Заявка на верификацию #{} одобрена админом {}", requestId, adminChatId);
    }

    /**
     * 🔥 Отклонить заявку
     */
    @Transactional
    public void rejectVerification(Long requestId, Long adminChatId, String comment) {
        VerificationRequest request = verificationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        if (request.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("Заявка уже обработана");
        }

        // Обновляем статус заявки
        request.setStatus(VerificationStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminChatId);
        request.setAdminComment(comment);

        verificationRepository.save(request);

        // Отправляем событие для уведомления пользователя
        eventPublisher.publishEvent(new VerificationStatusChangedEvent(request, adminChatId));

        log.info("❌ Заявка на верификацию #{} отклонена админом {}", requestId, adminChatId);
    }

    /**
     * 🔥 Получить заявку по ID
     */
    public Optional<VerificationRequest> getVerificationRequestById(Long requestId) {
        return verificationRepository.findById(requestId);
    }

    /**
     * 🔥 Получить количество заявок на рассмотрении
     */
    public long countPendingVerifications() {
        return verificationRepository.countByStatus(VerificationStatus.PENDING);
    }

    /**
     * 🔥 Проверить, верифицирован ли пользователь
     */
    public boolean isUserVerified(Long userChatId) {
        return verificationRepository.existsByUserChatIdAndTypeAndStatus(
                userChatId,
                VerificationType.GITHUB,
                VerificationStatus.APPROVED);
    }

    /**
     * 🔥 Получить информацию о пользователе для админа (если нужно)
     */
    public Optional<UserDto> getUserInfoForVerification(Long requestId) {
        Optional<VerificationRequest> request = verificationRepository.findById(requestId);
        if (request.isEmpty()) {
            return Optional.empty();
        }

        return userService.getUserDtoByChatId(request.get().getUserChatId());
    }

    public boolean canSendRequest(Long chatId) {
        VerificationStatus status = getGitHubVerificationStatus(chatId);

        // 1. Никогда не отправлял заявку
        if (status == null) {
            log.debug("Пользователь {} может отправить заявку: никогда не отправлял", chatId);
            return true;
        }

        // 2. Заявка на рассмотрении
        if (status == VerificationStatus.PENDING) {
            log.debug("Пользователь {} НЕ может отправить заявку: уже есть активная", chatId);
            return false;
        }

        // 3. Заявка одобрена (уже верифицирован)
        if (status == VerificationStatus.APPROVED) {
            log.debug("Пользователь {} НЕ может отправить заявку: уже верифицирован", chatId);
            return false;
        }

        if (status == VerificationStatus.REJECTED) {
            VerificationRequest request = getCurrentGitHubVerificationRequest(chatId)
                    .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

            if (request.getReviewedAt() == null) {
                return true;
            }

            return LocalDateTime.now()
                    .isAfter(request.getReviewedAt().plusMinutes(3));
        }
        else {
            log.debug("Ошибка кнопки верификации!");
            return false;
        }
    }
}