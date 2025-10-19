package com.devlink.devlink.repository;

import com.devlink.devlink.model.Project;
import com.devlink.devlink.model.ProjectStatus;
import com.devlink.devlink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Найти проекты по заказчику
    List<Project> findByCustomerOrderByCreatedAtDesc(User customer);

    // Найти проекты по исполнителю
    List<Project> findByFreelancerOrderByCreatedAtDesc(User freelancer);

    // Найти открытые проекты
    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    // Найти проекты по статусу и заказчику
    List<Project> findByCustomerAndStatusOrderByCreatedAtDesc(User customer, ProjectStatus status);

    // Поиск проектов по ключевым словам в названии и описании
    @Query("SELECT p FROM Project p WHERE p.status = 'OPEN' AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.requiredSkills) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY p.createdAt DESC")
    List<Project> searchOpenProjects(@Param("query") String query);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Project p WHERE p.id = :projectId AND p.customer.chatId = :chatId")
    boolean isProjectCustomer(@Param("projectId") Long projectId, @Param("chatId") Long chatId);

    @Query("SELECT p FROM Project p WHERE p.freelancer.chatId = :chatId ORDER BY p.createdAt DESC")
    List<Project> findProjectsByFreelancerChatId(@Param("chatId") Long chatId);

    @Query("UPDATE Project p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :projectId")
    void incrementViewsCount(@Param("projectId") Long projectId);

}
