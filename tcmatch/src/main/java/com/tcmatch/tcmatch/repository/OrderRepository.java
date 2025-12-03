package com.tcmatch.tcmatch.repository;

import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.OrderStatus;
import com.tcmatch.tcmatch.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Вспомогательный метод: Заказ всегда привязан к одному отклику
    Optional<Order> findByApplicationId(Long applicationId);

    // Получить все заказы, где пользователь - Заказчик
    // (chatId - это то, что мы используем для связи)
    List<Order> findAllByCustomerChatId(Long customerChatId);

    // Получить все заказы, где пользователь - Исполнитель
    List<Order> findAllByFreelancerChatId(Long freelancerChatId);

    // 🔥 НОВЫЙ МЕТОД: Поиск заказа по ID проекта
    // (Предполагаем, что у проекта может быть только один АКТИВНЫЙ заказ)
    Optional<Order> findByProjectIdAndStatus(Long projectId, OrderStatus status);
}
