package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.*;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProjectService projectService;
    private final UserService userService;

    public Application createApplication(Long projectId, Long freelancerChatId,
                                         String coverLetter, Double proposedBudget,
                                         Integer proposedDays) {
        Project project = projectService.getProjectById(projectId).orElseThrow(() -> new RuntimeException(("Пользователь не найден")));

        User freelancer = userService.findByChatId(freelancerChatId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (project.getCustomer().getChatId().equals(freelancerChatId)) {
            throw new RuntimeException("Нельзя откликнуться на свой же проект");
        }

        if (applicationRepository.existsByProjectAndFreelancerChatId(projectId, freelancerChatId)) {
            throw new RuntimeException("Вы уже откликались на этот проект");
        }

        if (project.getStatus() != UserRole.ProjectStatus.OPEN) {
            throw new RuntimeException("Проект уже закрыт для откликов");
        }

        Application application = Application.builder()
                .project(project)
                .freelancer(freelancer)
                .coverLetter(coverLetter)
                .proposedBudget(proposedBudget != null ? proposedBudget: project.getBudget())
                .proposedDays(proposedDays != null ? proposedDays : project.getEstimatedDays())
                .build();

        Application savedApplication = applicationRepository.save(application);

        project.setApplicationsCount(project.getApplicationsCount() + 1);

        log.info("✅ Создан отклик на проект {} от пользователя {}", projectId, freelancerChatId);
        return savedApplication;
    }

    public List<Application> getProjectApplications(Long projectId) {
        Project project = projectService.getProjectById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));
        return applicationRepository.findByProjectOrderByAppliedAtDesc(project);
    }

    public List<Application> getUserApplications(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return applicationRepository.findByFreelancerWithProjectAndCustomer(user);
    }

    public Application acceptApplication(Long applicationId, Long customerChatId) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Отклик не найден"));

        if (!application.getProject().getCustomer().getChatId().equals(customerChatId)) {
            throw new RuntimeException("Только заказчик может принимать отклики");
        }

        // Назначаем исполнителя проекту
        application.setStatus(UserRole.ApplicationStatus.ACCEPTED);
        application.setReviewedAt(LocalDateTime.now());

        projectService.assignFreelancer(application.getProject().getId(),
                application.getFreelancer().getChatId());

        // Отклоняем все остальные отклики на этот проект
        rejectOtherApplications(application.getProject().getId(), applicationId);

        Application savedApplication = applicationRepository.save(application);

        log.info("✅ Принят отклик {} на проект {}", applicationId, application.getProject().getId());
        return savedApplication;
    }

    public Application rejectApplication(Long applicationId, Long customerChatId, String comment) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Отклик не найден"));

        if (!application.getProject().getCustomer().getChatId().equals(customerChatId)) {
            throw new RuntimeException("Только заказчик может отклонять отклики");
        }

        application.setStatus(UserRole.ApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        application.setCustomerComment(comment);

        Application savedApplication = applicationRepository.save(application);

        log.info("✅ Отклонен отклик {} на проект {}", applicationId, application.getProject().getId());
        return savedApplication;
    }

    public void withdrawApplication(Long applicationId, Long freelancerChatId) {
        Application application = applicationRepository.findByIdWithProjectAndFreelancer(applicationId).orElseThrow(() -> new RuntimeException("Отклик не найден"));

        if (!application.getFreelancer().getChatId().equals(freelancerChatId)) {
            throw new RuntimeException("Только автор может отзывать отклик");
        }

        // 🔥 ПРОВЕРЯЕМ, ЧТО ОТКЛИК МОЖНО ОТОЗВАТЬ
        if (application.getStatus() != UserRole.ApplicationStatus.PENDING) {
            throw new RuntimeException("Нельзя отозвать отклик со статусом: " + application.getStatus());
        }

        // 🔥 ОБНОВЛЯЕМ СТАТУС ОТКЛИКА
        application.setStatus(UserRole.ApplicationStatus.WITHDRAWN);
        application.setReviewedAt(LocalDateTime.now());

        applicationRepository.save(application);

        // 🔥 УМЕНЬШАЕМ СЧЕТЧИК ОТКЛИКОВ ПРОЕКТА
        Project project = application.getProject();
        project.setApplicationsCount(Math.max(0, project.getApplicationsCount() - 1));
        projectService.updateProject(project);

        log.info("✅ Пользователь {} отозвал отклик {}", applicationId, freelancerChatId);
    }

    private void rejectOtherApplications(Long projectId, Long acceptedApplicationId) {
        List<Application> otherApplications = applicationRepository.findByProjectAndStatusOrderByAppliedAtDesc(
                projectService.getProjectById(projectId).get(), UserRole.ApplicationStatus.PENDING);

        for (Application application : otherApplications) {
            if (!application.getId().equals(acceptedApplicationId)) {
                application.setStatus(UserRole.ApplicationStatus.REJECTED);
                application.setReviewedAt(LocalDateTime.now());
                application.setCustomerComment("Проект уже назначен другому исполнителю");
            }
        }
        applicationRepository.saveAll(otherApplications);
    }

    public long getActiveApplicationsCount(Long chatId) {
        return applicationRepository.countActiveApplicationsByFreelancer(chatId);
    }

    public Optional<Application> getApplicationById(Long applicationId) {
        // 🔥 ИСПОЛЬЗУЕМ JOIN FETCH ДЛЯ ИЗБЕЖАНИЯ LAZY LOADING
        return applicationRepository.findByIdWithProjectAndFreelancer(applicationId);
    }

    // 🔥 ПОЛУЧЕНИЕ ID ПРОЕКТА ПО ID ОТКЛИКА
    public Long getProjectIdByApplicationId(Long applicationId) {
        Application application = getApplicationById(applicationId)
                .orElseThrow(() -> new RuntimeException("Отклик не найден"));
        return application.getProject().getId();
    }
}
