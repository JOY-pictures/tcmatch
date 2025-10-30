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

    // Найти заказы заказчика
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);

    // Найти заказы исполнителя
    List<Order> findByFreelancerOrderByCreatedAtDesc(User freelancer);

    // Найти заказы по статусу
    List<Order> findByStatusOrderByCreatedAtDesc(UserRole.OrderStatus status);

    // Найти заказы пользователя (как заказчика или исполнителя)
    @Query("SELECT o FROM Order o WHERE o.customer.chatId = :chatId OR o.freelancer.chatId = :chatId ORDER BY o.createdAt DESC")
    List<Order> findByUserChatId(@Param("chatId") Long chatId);


    // Найти активные заказы пользователя
    @Query("SELECT o FROM Order o WHERE (o.customer.chatId = :chatId OR o.freelancer.chatId = :chatId) " +
            "AND o.status IN ('CREATED', 'IN_PROGRESS', 'UNDER_REVIEW', 'REVISION') " +
            "ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByUserChatId(@Param("chatId") Long chatId);

    // Найти заказ по проекту
    Optional<Order> findByProjectId(Long projectId);

    // Найти заказ по заявке
    Optional<Order> findByApplicationId(Long applicationId);

    // Количество активных заказов пользователя
    @Query("SELECT COUNT(o) FROM Order o WHERE (o.customer.chatId = :chatId OR o.freelancer.chatId = :chatId) " +
            "AND o.status IN ('CREATED', 'IN_PROGRESS', 'UNDER_REVIEW', 'REVISION')")
    long countActiveOrdersByUserChatId(@Param("chatId") Long chatId);

    @Query("SELECT o FROM Order o WHERE o.deadline < CURRENT_TIMESTAMP AND o.status IN ('IN_PROGRESS', 'UNDER_REVIEW')")
    List<Order> findOverdueOrders();
}
