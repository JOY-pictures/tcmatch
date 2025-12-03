package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.VerificationStatus;
import com.tcmatch.tcmatch.model.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 ТОЛЬКО ID, а не сущность User
    @Column(name = "user_chat_id", nullable = false)
    private Long userChatId;

    @Column(name = "user_name")
    private String userName; // Для удобства админов

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    @Column(length = 500)
    private String providedData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    @Column(length = 500)
    private String adminComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VerificationStatus.PENDING;
        }
    }
}