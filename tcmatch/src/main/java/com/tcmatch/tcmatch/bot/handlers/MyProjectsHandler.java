//package com.tcmatch.tcmatch.bot.handlers;
//
//import com.tcmatch.tcmatch.bot.keyboards.KeyboardFactory;
//import com.tcmatch.tcmatch.model.dto.BaseHandlerData;
//import com.tcmatch.tcmatch.model.enums.UserRole;
//import com.tcmatch.tcmatch.service.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//
//@Component
//@Slf4j
//public class MyProjectsHandler extends BaseHandler {
//
//    private final UserService userService;
//    private final ProjectService projectService;
//    private final ApplicationService applicationService;
//    private final OrderService orderService;
//
//    public MyProjectsHandler(KeyboardFactory keyboardFactory,
//                             UserSessionService userSessionService,
//                             UserService userService,
//                             ProjectService projectService,
//                             ApplicationService applicationService,
//                             OrderService orderService) {
//        super(keyboardFactory, userSessionService);
//        this.userService = userService;
//        this.projectService = projectService;
//        this.applicationService = applicationService;
//        this.orderService = orderService;
//    }
//
//    @Override
//    public boolean canHandle(String actionType, String action) {
//        return "my_projects".equals(actionType);
//    }
//
//    @Override
//    public void handle(Long chatId, String action, String parameter, Integer messageId, String userName) {
//        BaseHandlerData data = new BaseHandlerData(chatId, messageId, userName);
//
//        switch (action) {
//            case "menu":
//                showMyProjectsMenu(data);
//                break;
//            case "role_select":
//                showRoleSelection(data);
//                break;
//            case "as_customer":
//                showCustomerProjects(data);
//                break;
//            case "as_freelancer":
//                showFreelancerProjects(data);
//                break;
//            case "project_details":
//                showCustomerProjectDetails(data, parameter);
//                break;
//            case "order_details":
//                showFreelancerOrderDetails(data, parameter);
//                break;
//            default:
//                log.warn("❌ Unknown my_projects action: {}", action);
//        }
//    }
//
//    public void showMyProjectsMenu(BaseHandlerData data) {
//        String text = """
//            📋 **МОИ ПРОЕКТЫ**
//
//            💼 Управление вашими проектами и заказами
//            """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createMyProjectsMenuKeyboard();
//        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
//    }
//
//    private void showRoleSelection(BaseHandlerData data) {
//        String text = """
//            👥 **ВЫБЕРИТЕ РОЛЬ**
//
//            Просмотр проектов и заказов в зависимости от вашей роли:
//            """;
//
//        InlineKeyboardMarkup keyboard = keyboardFactory.createRoleSelectionKeyboard();
//        editMessage(data.getChatId(), data.getMessageId(), text, keyboard);
//    }
//
//    private void showCustomerProjects(BaseHandlerData data) {
//        try {
//            User user = userService.findByChatId(data.getChatId())
//                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//
//            List<com.tcmatch.tcmatch.model.Project> userProjects = projectService.getUserProjects(data.getChatId());
//
//            if (userProjects.isEmpty()) {
//                String text = """
//                    👔 **КАК ЗАКАЗЧИК**
//
//                    📭 У вас пока нет созданных проектов
//
//                    💡 Создайте первый проект чтобы найти исполнителя
//                    """;
//                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackToMyProjectsKeyboard());
//                return;
//            }
//
//            // Показываем список проектов заказчика
//            showCustomerProjectsList(data, userProjects);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа проектов заказчика: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки проектов", 5);
//        }
//    }
//
//    private void showFreelancerProjects(BaseHandlerData data) {
//        try {
//            User user = userService.findByChatId(data.getChatId())
//                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//
//            List<com.tcmatch.tcmatch.model.Order> userOrders = orderService.getUserOrders(data.getChatId());
//
//            if (userOrders.isEmpty()) {
//                String text = """
//                    👨‍💻 **КАК ИСПОЛНИТЕЛЬ**
//
//                    📭 У вас пока нет активных заказов
//
//                    💡 Найдите проекты в поиске и отправьте отклики
//                    """;
//                editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackToMyProjectsKeyboard());
//                return;
//            }
//
//            // Показываем список заказов исполнителя
//            showFreelancerOrdersList(data, userOrders);
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка показа заказов исполнителя: {}", e.getMessage());
//            sendTemporaryErrorMessage(data.getChatId(), "Ошибка загрузки заказов", 5);
//        }
//    }
//
//    private void showCustomerProjectsList(BaseHandlerData data, List<com.tcmatch.tcmatch.model.Project> projects) {
//        StringBuilder text = new StringBuilder("""
//            👔 **ВАШИ ПРОЕКТЫ**
//
//            """);
//
//        for (int i = 0; i < Math.min(projects.size(), 10); i++) {
//            com.tcmatch.tcmatch.model.Project project = projects.get(i);
//            text.append("""
//                %d. %s%s
//                   💰 %.0f руб | ⏱️ %d дн. | %s
//
//                """.formatted(
//                    i + 1,
//                    getProjectStatusIcon(project.getStatus()),
//                    project.getTitle(),
//                    project.getBudget(),
//                    project.getEstimatedDays(),
//                    getProjectStatusDisplay(project.getStatus())
//            ));
//        }
//
//        if (projects.size() > 10) {
//            text.append("\n📊 ... и еще ").append(projects.size() - 10).append(" проектов");
//        }
//
//        editMessage(data.getChatId(), data.getMessageId(), text.toString(),
//                keyboardFactory.createCustomerProjectsKeyboard());
//    }
//
//    private void showCustomerProjectDetails(BaseHandlerData data, String projectId) {
//        // Реализуем позже - детали проекта заказчика
//        String text = "🚧 Детали проекта в разработке...";
//        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
//    }
//
//    private void showFreelancerOrderDetails(BaseHandlerData data, String orderId) {
//        // Реализуем позже - детали заказа исполнителя
//        String text = "🚧 Детали заказа в разработке...";
//        editMessage(data.getChatId(), data.getMessageId(), text, keyboardFactory.createBackButton());
//    }
//
//    // 🔥 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФОРМАТИРОВАНИЯ
//
//    private String getProjectStatusIcon(UserRole.ProjectStatus status) {
//        return switch (status) {
//            case OPEN -> "🔓 ";
//            case IN_PROGRESS -> "⚙️ ";
//            case COMPLETED -> "✅ ";
//            case CANCELLED -> "❌ ";
//            default -> "📁 ";
//        };
//    }
//
//    private String getProjectStatusDisplay(UserRole.ProjectStatus status) {
//        return switch (status) {
//            case OPEN -> "Открыт";
//            case IN_PROGRESS -> "В работе";
//            case COMPLETED -> "Завершен";
//            case CANCELLED -> "Отменен";
//            default -> "Неизвестно";
//        };
//    }
//
//    private String getOrderStatusIcon(UserRole.OrderStatus status) {
//        return switch (status) {
//            case CREATED -> "📦 ";
//            case IN_PROGRESS -> "⚙️ ";
//            case UNDER_REVIEW -> "👀 ";
//            case COMPLETED -> "✅ ";
//            case CANCELLED -> "❌ ";
//            case REVISION -> "🔄 ";
//            case AWAITING_CLARIFICATION -> "❓ ";
//            default -> "📁 ";
//        };
//    }
//
//    private String getOrderStatusDisplay(UserRole.OrderStatus status) {
//        return switch (status) {
//            case CREATED -> "Создан";
//            case IN_PROGRESS -> "В работе";
//            case UNDER_REVIEW -> "На проверке";
//            case COMPLETED -> "Завершен";
//            case CANCELLED -> "Отменен";
//            case REVISION -> "Правки";
//            case AWAITING_CLARIFICATION -> "Уточнение";
//            default -> "Неизвестно";
//        };
//    }
//
//    private String formatDays(Integer days) {
//        return days != null ? days + " дн." : "не указано";
//    }
//}
