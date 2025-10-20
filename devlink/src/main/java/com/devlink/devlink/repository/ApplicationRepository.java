package com.devlink.devlink.repository;

import com.devlink.devlink.model.Application;
import com.devlink.devlink.model.ApplicationStatus;
import com.devlink.devlink.model.Project;
import com.devlink.devlink.model.User;
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
    List<Application> findByProjectAndStatusOrderByAppliedAtDesc(Project project, ApplicationStatus status);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Application a WHERE a.project.id = :projectId And a.freelancer.chatId = :chatId")
    boolean existsByProjectAndFreelancerChatId(@Param("project") Long projectId, @Param("chatId") Long chatId);

    // Количество откликов на проект
    @Query("SELECT COUNT(a) FROM Application a WHERE a.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);

    // Количество активных откликов исполнителя
    @Query("SELECT COUNT(a) FROM Application a WHERE a.freelancer.chatId = :chatId AND a. status = 'PENDING'")
    long countActiveApplicationsByFreelancer(@Param("chatId") long chatId);
}
