package com.tcmatch.tcmatch.service;


import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    private Project createProject(Long customerChatId, String title, String description,
                                  Double budget, LocalDateTime deadline, String requiredSkills,
                                  Integer estimateDays) {
        User customer = userService.findByChatId(customerChatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем, что пользователь имеет право создавать проекты
        if (!userService.hasFullAccess(customerChatId)) {
            throw new RuntimeException("Для создания проектов необходимо завершить регистрацию");
        }

        Project project = Project.builder()
                .title(title)
                .description(description)
                .budget(budget)
                .customer(customer)
                .deadline(deadline)
                .requiredSkills(requiredSkills)
                .estimatedDays(estimateDays)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("✅ Создан новый проект: {} пользователем {}", title, customerChatId);
        return savedProject;
    }

    public List<Project> getOpenProjects() {
        return projectRepository.findByStatusOrderByCreatedAtDesc(UserRole.ProjectStatus.OPEN);
    }

    public List<Project> searchProjects(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            // 🔥 ПУСТАЯ СТРОКА - ВОЗВРАЩАЕМ ПУСТОЙ СПИСОК (ДЛЯ ИНТЕРФЕЙСА ПОИСКА)
            return Collections.emptyList();
        } else if ("all".equals(filter.trim())) {
            // 🔥 "all" - ВОЗВРАЩАЕМ ВСЕ ОТКРЫТЫЕ ПРОЕКТЫ
            return getOpenProjects();
        } else {
            // 🔥 ДРУГИЕ ФИЛЬТРЫ - ВЫПОЛНЯЕМ ПОИСК
            return applySpecialFilters(filter.trim());
        }
    }

    private List<Project> applySpecialFilters(String filter) {
        List<Project> allOpenProjects = getOpenProjects();

        if (filter.startsWith("budget:")) {
            return filterByBudget(allOpenProjects, filter);
        } else if ("urgent".equals(filter)) {
            return filterUrgentProjects(allOpenProjects);
        } else if ("junior".equals(filter)) {
            return filterJuniorProjects(allOpenProjects);
        } else {
            // 🔥 ЕСЛИ НЕ СПЕЦИАЛЬНЫЙ ФИЛЬТР - ИЩЕМ ПО ТЕКСТУ
            return projectRepository.searchOpenProjects(filter);
        }
    }

    private List<Project> filterByBudget(List<Project> projects, String budgetFilter) {
        try {
            // Извлекаем число из "budget:10000"
            String budgetStr = budgetFilter.substring("budget:".length());
            double maxBudget = Double.parseDouble(budgetStr);

            return projects.stream()
                    .filter(project -> project.getBudget() != null && project.getBudget() <= maxBudget)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Ошибка парсинга бюджета из фильтра: {}", budgetFilter);
            return Collections.emptyList();
        }
    }

    private List<Project> filterUrgentProjects(List<Project> projects) {
        // 🔥 СРОЧНЫЕ ПРОЕКТЫ - те, у которых срок меньше 7 дней
        return projects.stream()
                .filter(project -> project.getEstimatedDays() != null && project.getEstimatedDays() <= 7)
                .collect(Collectors.toList());
    }

    private List<Project> filterJuniorProjects(List<Project> projects) {
        // 🔥 ПРОЕКТЫ ДЛЯ НАЧИНАЮЩИХ - бюджет до 15000
        return projects.stream()
                .filter(project -> project.getBudget() != null && project.getBudget() <= 15000)
                .collect(Collectors.toList());
    }

    public List<Project> getUserProjects(Long chatId) {
        User user = userService.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 🔥 ИСПОЛЬЗУЕМ МЕТОД С JOIN FETCH
        return projectRepository.findByCustomerWithApplications(user);
    }

    public List<Project> getFreelancerProjects(Long chatId) {
        return projectRepository.findProjectsByFreelancerChatId(chatId);
    }

    public Optional<Project> getProjectById(Long projectId) {
        // 🔥 ИСПОЛЬЗУЕМ МЕТОД С JOIN FETCH
        Optional<Project> project = projectRepository.findByIdWithCustomerAndFreelancer(projectId);

        return project;
    }

    // 🔥 ОТДЕЛЬНЫЙ МЕТОД С НОВОЙ ТРАНЗАКЦИЕЙ
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewsCountInNewTransaction(Long projectId) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isPresent()) {
                Project project = projectOpt.get();
                project.setViewsCount(project.getViewsCount() + 1);
                projectRepository.save(project);
                log.debug("✅ Увеличено кол-во просмотров проекта {}: {}", projectId, project.getViewsCount());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка инкремента просмотров: {}", e.getMessage());
        }
    }

    public boolean isProjectCustomer(Long projectId, Long  chatId) {
        return projectRepository.isProjectCustomer(projectId, chatId);
    }

    public Project updateProjectStatus(Long projectId, UserRole.ProjectStatus newStatus) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));
        project.setStatus(newStatus);

        if (newStatus == UserRole.ProjectStatus.IN_PROGRESS) {
            project.setStartedAt(LocalDateTime.now());
        } else if (newStatus == UserRole.ProjectStatus.COMPLETED) {
            project.setCompletedAt(LocalDateTime.now());
        }
        return projectRepository.save(project);
    }

    /**
     * Обновляет только определенные поля проекта
     * Защита от случайного изменения важных данных
     */
    public Project updateProjectFields(Long projectId, String title, String description,
                                       Double budget, LocalDateTime deadline, String requiredSkills) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        // Обновляем только разрешенные поля
        if (title != null) project.setTitle(title);
        if (description != null) project.setDescription(description);
        if (budget != null) project.setBudget(budget);
        if (deadline != null) project.setDeadline(deadline);
        if (requiredSkills != null) project.setRequiredSkills(requiredSkills);

        Project updatedProject = projectRepository.save(project);
        log.info("✅ Поля проекта обновлены: {}", projectId);
        return updatedProject;
    }

    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    public Project assignFreelancer(Long projectId, long freelancerChatId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));

        User freelancer = userService.findByChatId(freelancerChatId).orElseThrow(()-> new RuntimeException("Исполнитель не найден"));

        project.setFreelancer(freelancer);
        project.setStatus(UserRole.ProjectStatus.IN_PROGRESS);
        project.setStartedAt(LocalDateTime.now());

        return projectRepository.save(project);
    }

    // 🔥 МЕТОД ДЛЯ УВЕЛИЧЕНИЯ ПРОСМОТРОВ (ВЫЗЫВАЕТСЯ ТОЛЬКО ЧЕРЕЗ ProjectViewService)
    @Transactional
    public void incrementProjectViews(Long projectId) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isPresent()) {
                Project project = projectOpt.get();
                project.setViewsCount(project.getViewsCount() + 1);
                projectRepository.save(project);
                log.debug("✅ Увеличено кол-во просмотров проекта {}: {}", projectId, project.getViewsCount());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка инкремента просмотров: {}", e.getMessage());
        }
    }
}