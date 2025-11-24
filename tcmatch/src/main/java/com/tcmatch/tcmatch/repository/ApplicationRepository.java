package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Найти отклики по проекту и статусу
    List<Application> findByProjectIdAndStatusOrderByAppliedAtDesc(Long projectId, UserRole.ApplicationStatus status);

    // Проверка существования отклика
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Application a WHERE a.projectId = :projectId AND a.freelancerChatId = :chatId")
    boolean existsByProjectAndFreelancerChatId(@Param("projectId") Long projectId, @Param("chatId") Long chatId);

    // Количество откликов на проект
    @Query("SELECT COUNT(a) FROM Application a WHERE a.projectId = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    // Количество активных откликов исполнителя
    @Query("SELECT COUNT(a) FROM Application a WHERE a.freelancerChatId = :chatId AND a.status = 'PENDING'")
    long countActiveApplicationsByFreelancer(@Param("chatId") long chatId);

    // 🔥 ДОБАВЛЯЕМ ЭТОТ МЕТОД
    List<Application> findByFreelancerChatId(Long freelancerChatId);

    // 🔥 ЕСЛИ ЕСТЬ - ОСТАВЛЯЕМ, ЕСЛИ НЕТ - ДОБАВЛЯЕМ
    List<Application> findByProjectIdOrderByAppliedAtDesc(Long projectId);

    List<Application> findByProjectIdIn(List<Long> projectIds);

    // 🔥 ДОБАВЛЯЕМ МЕТОД ДЛЯ ПОИСКА ПО СПИСКУ ID
    List<Application> findByProjectId(Long projectId);

    List<Application> findByFreelancerChatIdOrderByAppliedAtDesc(Long chatId);
}
