package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Найти проекты по заказчику
    List<Project> findByCustomerOrderByCreatedAtDesc(User customer);

    // Найти проекты по исполнителю
    List<Project> findByFreelancerOrderByCreatedAtDesc(User freelancer);

    // Найти открытые проекты
    List<Project> findByStatusOrderByCreatedAtDesc(UserRole.ProjectStatus status);

    // Найти проекты по статусу и заказчику
    List<Project> findByCustomerAndStatusOrderByCreatedAtDesc(User customer, UserRole.ProjectStatus status);

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

    @Modifying
    @Query("UPDATE Project p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :projectId")
    void incrementViewsCount(@Param("projectId") Long projectId);

    // 🔥 МЕТОД С ЯВНЫМ FETCH ПОЛЬЗОВАТЕЛЯ
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.customer WHERE p.id = :projectId")
    Optional<Project> findByIdWithCustomer(@Param("projectId") Long projectId);
}
