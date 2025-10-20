package com.devlink.devlink.service;


import com.devlink.devlink.model.Project;
import com.devlink.devlink.model.ProjectStatus;
import com.devlink.devlink.model.User;
import com.devlink.devlink.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        return projectRepository.findByStatusOrderByCreatedAtDesc(ProjectStatus.OPEN);
    }

    public List<Project> searchProjects(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getOpenProjects();
        }
        return projectRepository.searchOpenProjects(query.trim());
    }

    public List<Project> getUserProjects(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return projectRepository.findByCustomerOrderByCreatedAtDesc(user);
    }

    public List<Project> getFreelancerProjects(Long chatId) {
        return projectRepository.findProjectsByFreelancerChatId(chatId);
    }

    public Optional<Project> getProjectById(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        project.ifPresent(p -> projectRepository.incrementViewsCount(projectId));
        return project;
    }

    public boolean isProjectCustomer(Long projectId, Long  chatId) {
        return projectRepository.isProjectCustomer(projectId, chatId);
    }

    public Project updateProjectStatus(Long projectId, ProjectStatus newStatus) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));
        project.setStatus(newStatus);

        if (newStatus == ProjectStatus.IN_PROGRESS) {
            project.setStartedAt(LocalDateTime.now());
        } else if (newStatus == ProjectStatus.COMPLETED) {
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
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setStartedAt(LocalDateTime.now());

        return projectRepository.save(project);
    }

}