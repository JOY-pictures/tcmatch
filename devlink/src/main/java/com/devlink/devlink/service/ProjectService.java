package com.devlink.devlink.service;


import com.devlink.devlink.model.Project;
import com.devlink.devlink.model.User;
import com.devlink.devlink.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}