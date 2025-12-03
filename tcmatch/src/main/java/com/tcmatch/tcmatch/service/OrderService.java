package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.*;
import com.tcmatch.tcmatch.model.*;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.OrderCreationState;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.enums.OrderStatus;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final UserService userService;
    private final ReputationService reputationService;

    /**
     * Создает новый заказ на основе данных из мастера (state) и принятого отклика.
     * После создания меняет статус отклика на ACCEPTED.
     * @param state - DTO с данными о типе оплаты и этапах.
     * @return Созданная сущность Order.
     */
    @Transactional
    public Order createOrderFromState(OrderCreationState state) {

        // 1. Проверка существования отклика и его получение
        Long applicationId = state.getApplicationId();
        ApplicationDto application = applicationService.getApplicationDtoById(applicationId);
        // 2. Дополнительная проверка на дубликат (на случай сбоя в мастере)
        if (orderRepository.findByApplicationId(applicationId).isPresent()) {
            throw new IllegalStateException("Заказ для отклика ID:" + applicationId + " уже существует.");
        }

        // 3. Создание сущности Order
        Order order = Order.builder()
                .projectId(state.getProjectId())
                .applicationId(applicationId)
                .customerChatId(state.getCustomerChatId())
                .freelancerChatId(application.getFreelancerChatId())
                .totalBudget(application.getProposedBudget())
                .estimatedDays(application.getProposedDays())
                .paymentType(state.getPaymentType())
                .milestoneCount(state.getMilestoneCount())
                .status(OrderStatus.ACTIVE) // Новый заказ сразу активен
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now()) // Считаем, что старт = создание
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("✅ Создан новый заказ ID: {} по отклику ID: {}", savedOrder.getId(), applicationId);

        // 4. 🔥 КРИТИЧЕСКИ ВАЖНЫЙ ШАГ: Меняем статус отклика
        // Это запустит твой Наблюдатель (ApplicationStatusChangedEvent)
        // и уведомит исполнителя.
        applicationService.acceptApplication(applicationId, order.getCustomerChatId());

        return savedOrder;
    }

    // ==========================================================
    // 🔥 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (для дальнейшего развития)
    // ==========================================================

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Метод для завершения заказа (например, после оплаты последнего этапа).
     */
    @Transactional
    public void completeOrder(Long orderId) {
        Order order = getOrderById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден по ID: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("Attempt to complete already completed order ID: {}", orderId);
            return;
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("✅ Заказ ID: {} успешно завершен.", orderId);
    }

    /**
     * Получить активные заказы для Заказчика.
     */
    public List<Order> getActiveCustomerOrders(Long customerChatId) {
        return orderRepository.findAllByCustomerChatId(customerChatId)
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.ACTIVE)
                .toList();
    }

    /**
     * Получить активные заказы для Исполнителя.
     */
    public List<Order> getActiveFreelancerOrders(Long freelancerChatId) {
        return orderRepository.findAllByFreelancerChatId(freelancerChatId)
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.ACTIVE)
                .toList();
    }

    // 🔥 НОВЫЙ МЕТОД: (для Исполнителя)
    public Optional<Order> findByApplicationId(Long applicationId) {
        return orderRepository.findByApplicationId(applicationId);
    }

    // 🔥 НОВЫЙ МЕТОД: (для Заказчика)
    public Optional<Order> findActiveOrderByProjectId(Long projectId) {
        // Ищем заказ, который сейчас активен
        return orderRepository.findByProjectIdAndStatus(projectId, OrderStatus.ACTIVE);
    }

    //Создание заказа из принятой заявки
//    @Transactional
//    public Order createdOrderFromApplication(long applicationId) {
//        Application application = applicationService.getApplicationById(applicationId).orElseThrow(() -> new RuntimeException("Заявка не найдена"));
//
//        // Проверяем, что заявка принята
//        if (application.getStatus() != UserRole.ApplicationStatus.ACCEPTED) {
//            throw new RuntimeException("Нельзя создать заказ из непринятой заявки");
//        }
//
//        ProjectDto project = projectService.getProjectDtoById(application.getProjectId()).orElseThrow(() -> new RuntimeException("Проект не найден"));
//
//        // Проверяем, что проект еще открыт
//        if (project.getStatus() != UserRole.ProjectStatus.OPEN) {
//            throw new RuntimeException("Проект уже закрыт");
//        }
//
////        // Создаем этапы оплаты (по умолчанию: 30% аванс, 70% по завершении)
////
////        List<PaymentStage> paymentStages = createDefaultPaymentStages(project.getBudget());
////
////        Order order = Order.builder()
////                .project(project)
////                .application(application)
////                .customer(project.getCustomer())
////                .freelancer(project.getFreelancer())
////                .title(project.getTitle())
////                .description(project.getDescription())
////                .totalBudget(project.getBudget())
////                .estimatedDays(project.getEstimatedDays())
////                .customerRequirements(project.getDescription())
////                .paymentStages(paymentStages)
////                .deadline(LocalDateTime.now().plusDays(project.getEstimatedDays()))
////                .build();
////
////        projectService.updateProjectStatus(project.getId(), UserRole.ProjectStatus.IN_PROGRESS);
////
////        Order savedOrder = orderRepository.save(order);
////
////        log.info("✅ Создан заказ {} из заявки {} на проект {}", savedOrder.getId(), applicationId, project.getId());
////
////        return savedOrder;
//        return null;
//    }
//
//    //Создание этапов оплаты по умолчанию
//    private List<PaymentStage> createDefaultPaymentStages(Double totalBudget) {
//        List<PaymentStage> stages = new ArrayList<>();
//
//        // Аванс 30%
//        stages.add(PaymentStage.builder()
//                .name("Аванс")
//                .description("Предоплата за начало работы")
//                .amount(totalBudget * 0.3)
//                .percentage(30)
//                .build());
//
//        // Финальная оплата 70%
//        stages.add(PaymentStage.builder()
//                .name("Финальная оплата")
//                .description("оплата после приемки работы")
//                .amount(totalBudget * 0.7)
//                .percentage(70)
//                .build());
//        return stages;
//    }
//
//    //Начать работу над заказом
//    @Transactional
//    public Order startOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        if (order.getStatus() != UserRole.OrderStatus.CREATED) {
//            throw new RuntimeException("Заказ уже начат или завершен");
//        }
//
//        order.setStatus(UserRole.OrderStatus.IN_PROGRESS);
//        order.setStartedAt(LocalDateTime.now());
//
//        Order updatedOrder = orderRepository.save(order);
//        log.info("🚀 Заказ {} начат", orderId);
//
//        return updatedOrder;
//    }
//
//
//    //Отправить работу на проверку
//    @Transactional
//    public Order submitWork(Long orderId, String workResult) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        if (order.getStatus() != UserRole.OrderStatus.IN_PROGRESS) {
//            throw new RuntimeException("Заказ не в процессе выполнения");
//        }
//
//        order.setStatus(UserRole.OrderStatus.UNDER_REVIEW);
//        order.setWorkResult(workResult);
//
//        Order updatedOrder = orderRepository.save(order);
//        log.info("📤 Работа по заказу {} отправлена на проверку", orderId);
//
//        return updatedOrder;
//    }
//
//    //Принять работу
//    @Transactional
//    public Order acceptWork(Long orderId, Long customerChatId) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        // Проверяем, что пользователь является заказчиком
//        if (!order.getCustomerChatId().equals(customerChatId)) {
//            throw new RuntimeException("Только заказчик может принимать работу");
//        }
//
//        if (order.getStatus() != UserRole.OrderStatus.UNDER_REVIEW) {
//            throw new RuntimeException("Работа не на проверке");
//        }
//
//        if (isMinimumTimePassed(order)) {
//            throw new RuntimeException("Сделка не может быть завершена раньше чем через 48 часов после создания");
//        }
//
//
//        order.setStatus(UserRole.OrderStatus.COMPLETED);
//        order.setCompletedAt(LocalDateTime.now());
//
//        // Помечаем все этапы оплаты как выполненные
//        order.getPaymentStages().forEach(stage -> {
//            stage.setIsCompleted(true);
//            stage.setCompletedAt(LocalDateTime.now());
//        });
//
//        Order updatedOrder = orderRepository.save(order);
//
//        boolean isOnTime = !LocalDateTime.now().isAfter(order.getDeadline());
//        reputationService.updateUserReputation(
//                order.getFreelancerChatId(),
//                order.getProjectId(),
//                true, // успешное завершение
//                isOnTime,
//                order.getTotalBudget(),
//                false, // арбитраж
//                false  // не проигран
//        );
//
//        // Обновляем проект
//        projectService.updateProjectStatus(order.getProjectId(), UserRole.ProjectStatus.COMPLETED);
//        log.info("✅ Работа по заказу {} принята", orderId);
//        return updatedOrder;
//    }
//
//    //Отменить заказ
//    @Transactional
//    public Order cancelOrder(Long orderId, Long userChatId, String reason) {
//        Order order = orderRepository.findById(userChatId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        // Проверяем, что пользователь является участником заказа
//
//        if (!order.getCustomerChatId().equals(userChatId) &&
//                !order.getFreelancerChatId().equals(userChatId)) {
//            throw new RuntimeException("Только участники заказа могут его отменять");
//        }
//
//        order.setStatus(UserRole.OrderStatus.CANCELLED);
//
//        projectService.updateProjectStatus(order.getProjectId(), UserRole.ProjectStatus.OPEN);
//
//        Order updatedOrder = orderRepository.save(order);
//        log.info("❌ Заказ {} отменен по причине: {}", orderId, reason);
//
//        return updatedOrder;
//    }
//
//    //Запросить правки
//    @Transactional
//    public Order requestRevision(Long orderId, Long customerChatId, String revisionNotes) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        // Проверяем, что пользователь является заказчиком
//        if (!order.getCustomerChatId().equals(customerChatId)) {
//            throw new RuntimeException("Только заказчик может запрашивать правки");
//        }
//
//        if (order.getStatus() != UserRole.OrderStatus.UNDER_REVIEW) {
//            throw new RuntimeException("Работа не на проверке");
//        }
//
//        if (order.getRevisionCount() >= order.getMaxRevisions()) {
//            throw new RuntimeException("Достигнут лимит правок");
//        }
//
//        order.setStatus(UserRole.OrderStatus.REVISION);
//        int newRevisionCount = order.getRevisionCount();
//        order.setRevisionCount(newRevisionCount + 1);
//
//        // Сохраняем текущие комментарии к правке
//        order.setCurrentRevisionNotes(revisionNotes);
//
//        // Добавляем в историю правок
//        RevisionNote revisionNote = RevisionNote.builder()
//                .notes(revisionNotes)
//                .createdAt(LocalDateTime.now())
//                .revisionNumber(newRevisionCount)
//                .requestedBy("CUSTOMER")
//                .build();
//
//        order.getRevisionHistory().add(revisionNote);
//
//        Order updateOrder = orderRepository.save(order);
//        log.info("🔄 Запрошены правки по заказу {} (правка #{})", orderId, newRevisionCount);
//        return updateOrder;
//    }
//
//    @Transactional
//    public Order markRevisionResolved(Long orderId, Long freelancerChatId, String workResult) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        // Проверяем, что пользователь является исполнителем
//        if (!order.getFreelancerChatId().equals(freelancerChatId)) {
//            throw new RuntimeException("Только исполнитель может отмечать правки как исправленные");
//        }
//
//        if (order.getStatus() != UserRole.OrderStatus.REVISION) {
//            throw new RuntimeException("Заказ не в статусе правок");
//        }
//
//        // Отмечаем последнюю правку как исправленную
//        if (!order.getRevisionHistory().isEmpty()) {
//            RevisionNote lastRevision = order.getRevisionHistory().get(order.getRevisionCount() - 1);
//            lastRevision.setIsResolved(true);
//            lastRevision.setCreatedAt(LocalDateTime.now());
//        }
//
//        if (workResult != null) {
//            order.setWorkResult(workResult);
//        }
//
//        // Очищаем текущие комментарии к правке
//        order.setCurrentRevisionNotes(null);
//
//        // Возвращаем на проверку
//        order.setStatus(UserRole.OrderStatus.UNDER_REVIEW);
//
//        Order updateOrder = orderRepository.save(order);
//        log.info("✅ Исполнитель отметил правку как исправленную по заказу {}", orderId);
//
//        return updateOrder;
//    }
//
//    @Transactional
//    public Order requestClarification(Long orderId, Long freelancerChatId, String question) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        if (!order.getFreelancerChatId().equals(freelancerChatId)) {
//            throw new RuntimeException("Только исполнитель может запрашивать уточнения");
//        }
//
//        if (order.getStatus() != UserRole.OrderStatus.IN_PROGRESS) {
//            throw new RuntimeException("Уточнения можно запрашивать только во время работы");
//        }
//
//        if (order.getClarificationCount() >= order.getMaxClarifications()) {
//            throw new RuntimeException("Достигнут лимит уточнений (" + order.getMaxClarifications() + ")");
//        }
//
//        order.setStatus(UserRole.OrderStatus.AWAITING_CLARIFICATION);
//        int newClarificationCount = order.getClarificationCount() + 1;
//        order.setClarificationCount(newClarificationCount);
//
//        // Добавляем в историю как УТОЧНЕНИЕ
//        RevisionNote clarificationNote = RevisionNote.builder()
//                .notes(question)
//                .createdAt(LocalDateTime.now())
//                .revisionNumber(newClarificationCount)
//                .requestedBy("FREELANCER")
//                .noteType("CLARIFICATION")
//                .build();
//        order.getRevisionHistory().add(clarificationNote);
//        Order updatedOrder = orderRepository.save(order);
//
//        log.info("❓ Запрошено УТОЧНЕНИЕ по заказу {} (уточнение #{})", orderId, newClarificationCount);
//        return updatedOrder;
//    }
//
//    //Обновить этап оплаты
//    @Transactional
//    public Order updatePaymentStage(Long orderId, Integer stageIndex, String paymentProof) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        if (stageIndex < 0 || stageIndex >= order.getPaymentStages().size()) {
//            throw new RuntimeException("Неверный индекс этапа");
//        }
//
//        PaymentStage stage = order.getPaymentStages().get(stageIndex);
//        stage.setIsPaid(true);
//        stage.setPaymentProof(paymentProof);
//        stage.setPaidAt(LocalDateTime.now());
//
//        Order updatedOrder = orderRepository.save(order);
//        log.info("💰 Этап {} заказа {} отмечен как оплаченный", stageIndex, orderId);
//        return updatedOrder;
//    }
//
//    @Transactional
//    public Order providedClarification(Long orderId, Long customerChatId, String answer) {
//        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        // Проверяем, что пользователь является заказчиком
//        if (!order.getCustomerChatId().equals(customerChatId)) {
//            throw new RuntimeException("Только заказчик может отвечать на уточнения");
//        }
//
//        if (order.getStatus() != UserRole.OrderStatus.AWAITING_CLARIFICATION) {
//            throw new RuntimeException("Заказ не ожидает уточнения");
//        }
//
//        Optional<RevisionNote> lastClarification = order.getRevisionHistory().stream().filter(note -> "CLARIFICATION".equals(note.getNoteType()) && !note.getIsResolved())
//                .reduce((first, second) -> second);
//
//        if (lastClarification.isPresent()) {
//            RevisionNote clarification = lastClarification.get();
//            clarification.setResolutionNotes(answer);
//            clarification.setIsResolved(true);
//            clarification.setResolvedAt(LocalDateTime.now());
//        }
//
//        order.setStatus(UserRole.OrderStatus.IN_PROGRESS);
//
//        Order updatedOrder = orderRepository.save(order);
//        log.info("✅ Дан ответ на уточнение по заказу {}", orderId);
//
//        return updatedOrder;
//    }
//
//    // 🛡️ ПРОВЕРКА НА МИНИМАЛЬНЫЙ СРОК (48 часов)
//    public boolean isMinimumTimePassed(Order order) {
//        LocalDateTime now = LocalDateTime.now();
//        long hoursBetween = ChronoUnit.HOURS.between(order.getCreatedAt(), now);
//        return hoursBetween >= 48;
//    }
//
//    // 🔍 ПРОВЕРКА ЛИМИТА СДЕЛОК МЕЖДУ ОДНИМИ И ТЕМИ ЖЕ ПОЛЬЗОВАТЕЛЯМИ
//    public boolean isWithinMonthlyLimit(Long freelancerId, Long customerId) {
//        // Здесь будет логика проверки лимита в 3 сделки в месяц
//        // Пока заглушка
//        return true;
//    }
//
//    //Получить заказы пользователя
//    public List<Order> getUserOrders(Long chatId) {
//        return orderRepository.findByUserChatId(chatId);
//    }
//
//    //Получить заказ по ID
//    public Optional<Order> getOrderById(Long orderId) {
//        return orderRepository.findById(orderId);
//    }
//
//    public long getActiveOrderCount(Long chatId) {
//        return orderRepository.countActiveOrdersByUserChatId(chatId);
//    }
}
