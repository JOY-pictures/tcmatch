package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Notification;
import com.tcmatch.tcmatch.model.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserChatIdOrderByCreatedAtDesc(Long userChatId, Pageable pageable);

    // ДЛЯ "УМНОГО ПУША"
    List<Notification> findByUserChatIdAndStatusOrderByCreatedAtDesc(Long userChatId, NotificationStatus status, Pageable pageable);

    // ДЛЯ ПОДСЧЕТА НЕПРОЧИТАННЫХ
    long countByUserChatIdAndStatus(Long userChatId, NotificationStatus status);

    // 🔥 НОВЫЙ МЕТОД: Получение ТОЛЬКО ID для пагинации
    @Query("SELECT n.id FROM Notification n WHERE n.userChatId = :userChatId ORDER BY n.createdAt DESC")
    List<Long> findIdByUserIdOrderByCreatedAtDesc(@Param("userChatId") Long userChatId);
}
