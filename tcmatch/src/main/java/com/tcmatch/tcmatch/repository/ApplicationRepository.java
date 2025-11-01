package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    // 🔥 НОВЫЙ МЕТОД С JOIN FETCH
    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.project p " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE a.freelancer = :freelancer " +
            "ORDER BY a.appliedAt DESC")
    List<Application> findByFreelancerWithProjectAndCustomer(@Param("freelancer") User freelancer);

    // 🔥 АНАЛОГИЧНО ДЛЯ ПРОЕКТНЫХ ОТКЛИКОВ
    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.freelancer " +
            "LEFT JOIN FETCH a.project p " +
            "WHERE p.id = :projectId " +
            "ORDER BY a.appliedAt DESC")
    List<Application> findByProjectWithFreelancer(@Param("projectId") Long projectId);

    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.project p " +
            "LEFT JOIN FETCH p.customer " +
            "LEFT JOIN FETCH a.freelancer " +
            "WHERE a.id = :id")
    Optional<Application> findByIdWithProjectAndFreelancer(@Param("id") Long id);
}
