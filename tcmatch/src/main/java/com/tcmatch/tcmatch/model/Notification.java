package com.tcmatch.tcmatch.model;

import com.tcmatch.tcmatch.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "user_chat_id", nullable = false)
    private Long userChatId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    private String callbackData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // КОНСТРУКТОР ДЛЯ СОЗДАНИЯ (принимает userId, а не User entity)
    public Notification(Long userChatId, String text, String callbackData) { // 🔥 ИЗМЕНЕНИЕ
        this.userChatId = userChatId; // 🔥 ИЗМЕНЕНИЕ
        this.text = text;
        this.callbackData = callbackData;
        this.status = NotificationStatus.UNREAD;
    }
}
