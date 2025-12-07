package com.tcmatch.tcmatch.config;

import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import com.tcmatch.tcmatch.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@Profile("dev") // 🔥 ТОЛЬКО ДЛЯ РАЗРАБОТКИ
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer {

    private final UserService userService;
    private final ProjectService projectService;
    private final ApplicationService applicationService;

    @PostConstruct
    public void init() {
        try {
            // Проверяем, есть ли уже тестовые данные
            if (projectService.getOpenProjects().isEmpty()) {
                log.info("🚀 Creating test data...");
                createTestData();
                log.info("✅ Тестовые данные успешно созданы");
                applicationService.createApplication(7L, 7965798029L, "Я готов в срок выполнить ваш заказ", 10000.0, 20);

            } else {
                log.info("✅ Тестовые данные уже существуют");
            }
        } catch (Exception e) {
            log.error("❌ Ошибка создания тестовых данных: {}", e.getMessage());
        }
    }

    private void createTestData() {
        User customer1 = createTestUser(111111111L, "customer1", "Алексей", "Попов", UserRole.CUSTOMER, "Business", "Management");
        User customer2 = createTestUser(222222222L, "customer2", "Мария", "Проджект", UserRole.CUSTOMER, "Startup", "Project Management");
        User niddyCustomer = createTestUser(5519912522L, "xN1DDYx", "Шероз", "Проджект", UserRole.CUSTOMER, "Business", "Project Management");

        User freelancer1 = createTestUser(333333333L, "freelancer1", "Дмитрий", "Разработчик", UserRole.FREELANCER, "Backend", "Java, Spring, PostgreSQL");
        User freelancer2 = createTestUser(444444444L, "freelancer2", "Анна", "Дизайнер", UserRole.FREELANCER, "Frontend", "React, JavaScript, UI/UX");
        User profitFreelancer = createTestUser(7965798029L, "Profity12", "Артур", "Программист", UserRole.FREELANCER, "Bckend", "Java");

        List<Project> testProjects = Arrays.asList(
                createProject(customer1, "Разработка CRM системы",
                        "Создание системы управления клиентами для малого бизнеса. Функции: ведение клиентской базы, история взаимодействий, напоминания.",
                        45000.0, 25, "Java, Spring Boot, PostgreSQL, React"),

                createProject(customer1, "Чат-бот для техподдержки",
                        "Разработка AI-бота для автоматизации ответов на частые вопросы клиентов. Интеграция с сайтом и Telegram.",
                        18000.0, 12, "Python, NLP, Telegram API, FastAPI"),

                createProject(customer2, "Мобильное приложение для фитнеса",
                        "Создание приложения с тренировками, питанием и отслеживанием прогресса. Интеграция с Google Fit/Apple Health.",
                        60000.0, 35, "Flutter, Dart, Firebase, REST API"),

                createProject(customer2, "Лендинг для образовательного курса",
                        "Разработка продающей страницы для онлайн-курса. Адаптивный дизайн, формы захвата, интеграция с платежами.",
                        12000.0, 8, "HTML, CSS, JavaScript, WordPress"),

                createProject(customer1, "Парсинг данных с маркетплейсов",
                        "Сбор информации о товарах, ценах и отзывах с различных маркетплейсов. Визуализация данных в дашборде.",
                        22000.0, 15, "Python, Selenium, Pandas, Data Visualization"),

                createProject(customer2, "API для сервиса доставки",
                        "Разработка backend для агрегатора служб доставки. Функции: расчет стоимости, отслеживание, уведомления.",
                        35000.0, 20, "Node.js, Express, MongoDB, WebSocket"),

                createProject(niddyCustomer, "API для сервиса доставки",
                        "Разработка backend для агрегатора служб доставки. Функции: расчет стоимости, отслеживание, уведомления.",
                        35000.0, 20, "Node.js, Express, MongoDB, WebSocket")
        );

        for (Project project : testProjects) {
            projectService.updateProject(project);
            log.info("✅ Создано тестовых данных: {} пользователей, {} проектов", 4, testProjects.size());
        }
    }
    private User createTestUser(Long chatId, String username, String firstname, String lastname, UserRole role, String specialization, String skills) {
            User user = userService.registerFromTelegram(chatId, username, firstname, lastname);

            userService.updateUserRole(chatId, role);
            user.setIsVerified(true);
            user.setVerificationMethod("TEST_DATA");
            userService.acceptRules(chatId);
            userService.updateProfessionalInfo(chatId, specialization, "Middle", skills);
            return user;

    }

    private Project createProject(User customer, String title, String description,
                                  Double budget, Integer days, String skills) {
        return Project.builder()
                .title(title)
                .description(description)
                .budget(budget)
                .customerChatId(customer.getChatId())
                .deadline(LocalDateTime.now().plusDays(days))
                .requiredSkills(skills)
                .estimatedDays(days)
                .viewsCount(0)
                .applicationsCount(0)
                .status(UserRole.ProjectStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

}
