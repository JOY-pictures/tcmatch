package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Найти отклики по проекту
    List<Application> findByProjectOrderByAppliedAtDesc(Project project);

    // Найти отклики исполнителя
    List<Application> findByFreelancerOrderByAppliedAtDesc(User freelancer);

    // Найти отклики по проекту и статусу
    List<Application> findByProjectAndStatusOrderByAppliedAtDesc(Project project, UserRole.ApplicationStatus status);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Application a WHERE a.project.id = :projectId And a.freelancer.chatId = :chatId")
    boolean existsByProjectAndFreelancerChatId(@Param("projectId") Long projectId, @Param("chatId") Long chatId);

    // Количество откликов на проект
    @Query("SELECT COUNT(a) FROM Application a WHERE a.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    // Количество активных откликов исполнителя
    @Query("SELECT COUNT(a) FROM Application a WHERE a.freelancer.chatId = :chatId AND a. status = 'PENDING'")
    long countActiveApplicationsByFreelancer(@Param("chatId") long chatId);
}
