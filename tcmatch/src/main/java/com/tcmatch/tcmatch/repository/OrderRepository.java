package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerChatIdOrderByCreatedAtDesc(Long customerChatId);
    List<Order> findByFreelancerChatIdOrderByCreatedAtDesc(Long freelancerChatId);

    // Найти заказы по статусу
    List<Order> findByStatusOrderByCreatedAtDesc(UserRole.OrderStatus status);

    // Найти заказ по проекту
    Optional<Order> findByProjectId(Long projectId);
    // Найти заказ по заявке
    Optional<Order> findByApplicationId(Long applicationId);

    @Query("SELECT o FROM Order o WHERE o.deadline < CURRENT_TIMESTAMP AND o.status IN ('IN_PROGRESS', 'UNDER_REVIEW')")
    List<Order> findOverdueOrders();

    // 🔥 ЭТОТ МЕТОД БУДЕТ РАБОТАТЬ - поля customerChatId и freelancerChatId существуют
    @Query("SELECT o FROM Order o WHERE o.customerChatId = :chatId OR o.freelancerChatId = :chatId ORDER BY o.createdAt DESC")
    List<Order> findByUserChatId(@Param("chatId") Long chatId);

    // 🔥 ОБЪЕДИНЕННЫЙ МЕТОД ДЛЯ ПОДСЧЕТА АКТИВНЫХ ЗАКАЗОВ ПОЛЬЗОВАТЕЛЯ
    @Query("SELECT COUNT(o) FROM Order o WHERE (o.customerChatId = :userChatId OR o.freelancerChatId = :userChatId) AND o.status IN ('CREATED', 'IN_PROGRESS', 'UNDER_REVIEW', 'REVISION')")
    long countActiveOrdersByUserChatId(@Param("userChatId") Long userChatId);
}
