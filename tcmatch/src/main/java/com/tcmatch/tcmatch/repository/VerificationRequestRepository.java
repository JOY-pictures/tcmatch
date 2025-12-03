package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.VerificationRequest;
import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.model.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    // 🔥 Теперь работаем с userChatId, а не userId

    Optional<VerificationRequest> findByUserChatIdAndTypeAndStatus(
            Long userChatId,
            VerificationType type,
            VerificationStatus status);

    List<VerificationRequest> findByUserChatIdOrderByCreatedAtDesc(Long userChatId);

    Optional<VerificationRequest> findTopByUserChatIdAndTypeOrderByCreatedAtDesc(
            Long userChatId,
            VerificationType type);

    List<VerificationRequest> findByStatusOrderByCreatedAtDesc(VerificationStatus status);

    List<VerificationRequest> findByTypeAndStatusOrderByCreatedAtDesc(
            VerificationType type,
            VerificationStatus status);

    boolean existsByUserChatIdAndTypeAndStatus(
            Long userChatId,
            VerificationType type,
            VerificationStatus status);

    Long countByStatus(VerificationStatus status);

    // Для поиска заявок по типу
    List<VerificationRequest> findByTypeOrderByCreatedAtDesc(VerificationType type);
}