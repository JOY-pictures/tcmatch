package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    // Найти проекты по customerChatId (вместо User customer)
    List<Project> findByCustomerChatIdOrderByCreatedAtDesc(Long customerChatId);

    // Найти проекты по freelancerChatId (вместо User freelancer)
    List<Project> findByFreelancerChatIdOrderByCreatedAtDesc(Long freelancerChatId);

    // Найти открытые проекты
    List<Project> findByStatusOrderByCreatedAtDesc(UserRole.ProjectStatus status);

    // Найти проекты по статусу и customerChatId
    List<Project> findByCustomerChatIdAndStatusOrderByCreatedAtDesc(Long customerChatId, UserRole.ProjectStatus status);

    // Поиск проектов по ключевым словам в названии и описании
    @Query("SELECT p FROM Project p WHERE p.status = 'OPEN' AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.requiredSkills) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY p.createdAt DESC")
    List<Project> searchOpenProjects(@Param("query") String query);

    // Проверка прав заказчика
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Project p WHERE p.id = :projectId AND p.customerChatId = :chatId")
    boolean isProjectCustomer(@Param("projectId") Long projectId, @Param("chatId") Long chatId);

    @Query("SELECT p FROM Project p WHERE p.freelancerChatId = :chatId ORDER BY p.createdAt DESC")
    List<Project> findProjectsByFreelancerChatId(@Param("chatId") Long chatId);

    @Modifying
    @Query("UPDATE Project p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :projectId")
    void incrementViewsCount(@Param("projectId") Long projectId);

    Optional<Project> findById(Long id);

    /**
     * 🔥 Реализация 1: Находит все проекты с указанным статусом,
     * отсортированные по дате создания в обратном порядке (от новых к старым).
     * @param status Статус проекта (например, "OPEN").
     */
    List<Project> findAllByStatusOrderByCreatedAtDesc(UserRole.ProjectStatus status);


    @Query("""
        SELECT p FROM Project p 
        WHERE p.status = 'OPEN' 
        AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minBudget IS NULL OR p.budget >= :minBudget)
        ORDER BY p.createdAt DESC
    """)
    List<Project> findActiveProjectsByFilters(
            @Param("keyword") String keyword,
            @Param("minBudget") Integer minBudget
    );

    @Query("SELECT p FROM Project p WHERE " +
            "p.status = 'OPEN' AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minBudget IS NULL OR p.budget >= :minBudget)")
    List<Project> findActiveProjectsByFilters(@Param("keyword") String keyword,
                                              @Param("requiredSkills") List<String> requiredSkills,
                                              @Param("minBudget") Integer minBudget);

    // 🔥 МЕТОД ДЛЯ ПОИСКА ПРОЕКТОВ ПОЛЬЗОВАТЕЛЯ (ЗАКАЗЧИКА)
    @Query("SELECT p FROM Project p WHERE p.customerChatId = :customerChatId")
    List<Project> findByCustomerChatId(@Param("customerChatId") Long customerChatId);

    // 🔥 МЕТОД ДЛЯ ПОИСКА ПО НАВЫКАМ (если нужно)
    @Query("SELECT p FROM Project p WHERE " +
            "p.status = 'OPEN' AND " +
            "(:requiredSkills IS NULL OR " +
            "LOWER(p.requiredSkills) LIKE LOWER(CONCAT('%', :skill, '%')))")
    List<Project> findByRequiredSkillsContaining(@Param("requiredSkills") String skill);

}
