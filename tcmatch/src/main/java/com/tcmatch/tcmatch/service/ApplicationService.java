package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.events.ApplicationStatusChangedEvent;
import com.tcmatch.tcmatch.events.NewApplicationEvent;
import com.tcmatch.tcmatch.model.*;
import com.tcmatch.tcmatch.model.Application;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectService projectService;
    private final UserService userService;

    @Lazy
    private final BotExecutor botExecutor;

    private final SubscriptionService subscriptionService;

    @Transactional
    public Application createApplication(Long projectId, Long freelancerChatId,
                                         String coverLetter, Double proposedBudget,
                                         Integer proposedDays) {
        Project project = projectService.getProjectById(projectId).orElseThrow(() -> new RuntimeException(("Пользователь не найден")));



        UserDto freelancer = userService.getUserDtoByChatId(freelancerChatId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Long customerChatId = projectService.getCustomerChatIdByProjectId(projectId);
        if (customerChatId.equals(freelancerChatId)) {
            throw new RuntimeException("Нельзя откликнуться на свой же проект");
        }

        if (applicationRepository.existsByProjectAndFreelancerChatId(projectId, freelancerChatId)) {
            throw new RuntimeException("Вы уже откликались на этот проект");
        }

        if (project.getStatus() != UserRole.ProjectStatus.OPEN) {
            throw new RuntimeException("Проект уже закрыт для откликов");
        }

        Application application = Application.builder()
                .projectId(projectId)
                .freelancerChatId(freelancerChatId)
                .coverLetter(coverLetter)
                .proposedBudget(proposedBudget != null ? proposedBudget: project.getBudget())
                .proposedDays(proposedDays != null ? proposedDays : project.getEstimatedDays())
                .build();

        Application savedApplication = applicationRepository.save(application);

        subscriptionService.decrementApplicationCount(freelancer.getChatId());

        ApplicationDto applicationDto = getApplicationDtoById(application.getId());

        project.setApplicationsCount(project.getApplicationsCount() + 1);

        eventPublisher.publishEvent(new NewApplicationEvent(applicationDto));

        log.info("✅ Создан отклик на проект {} от пользователя {}", projectId, freelancerChatId);
        return savedApplication;
    }

    public List<Application> getProjectApplications(Long projectId) {
        // 🔥 УБИРАЕМ ПРОВЕРКУ ПРОЕКТА И ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
        return applicationRepository.findByProjectIdOrderByAppliedAtDesc(projectId);
    }

    public List<Application> getUserApplications(Long chatId) {
        // 🔥 УБИРАЕМ ПРОВЕРКУ ПОЛЬЗОВАТЕЛЯ И ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
        return applicationRepository.findByFreelancerChatIdOrderByAppliedAtDesc(chatId);
    }

    @Transactional
    public Application acceptApplication(Long applicationId, Long customerChatId) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Отклик не найден"));

        Long projectCustomerChatId = projectService.getCustomerChatIdByProjectId(application.getProjectId());
        if (!customerChatId.equals(projectCustomerChatId)) {
            throw new RuntimeException("Только заказчик может принимать отклики");
        }

        // Назначаем исполнителя проекту
        application.setStatus(UserRole.ApplicationStatus.ACCEPTED);
        application.setReviewedAt(LocalDateTime.now());

        projectService.assignFreelancer(application.getProjectId(),
                application.getFreelancerChatId());

        // Отклоняем все остальные отклики на этот проект
        rejectOtherApplications(application.getProjectId(), applicationId);

        Application savedApplication = applicationRepository.save(application);

        // 🔥 1. КОНВЕРТИРУЕМ Entity В DTO
        ApplicationDto applicationDto = getApplicationDtoById(application.getId()); // Используй свой метод конвертации

        // 🔥 2. ПУБЛИКУЕМ СОБЫТИЕ С DTO
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(applicationDto, UserRole.ApplicationStatus.ACCEPTED));

        log.info("✅ Принят отклик {} на проект {}", applicationId, application.getProjectId());
        return savedApplication;
    }

    @Transactional
    public Application rejectApplication(Long applicationId, Long customerChatId, String comment) {
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Отклик не найден"));

        // 🔥 ПРОВЕРКА ПРАВ ЧЕРЕЗ СЕРВИС ПРОЕКТА
        Long projectCustomerChatId = projectService.getCustomerChatIdByProjectId(application.getProjectId());
        if (!projectCustomerChatId.equals(customerChatId)) {
            throw new RuntimeException("Только заказчик может отклонять отклики");
        }

        application.setStatus(UserRole.ApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        application.setCustomerComment(comment);

        Application savedApplication = applicationRepository.save(application);

        // 🔥 1. КОНВЕРТИРУЕМ Entity В DTO
        ApplicationDto applicationDto = getApplicationDtoById(application.getId()); // Используй свой метод конвертации

        // 🔥 2. ПУБЛИКУЕМ СОБЫТИЕ С DTO
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(applicationDto, UserRole.ApplicationStatus.REJECTED));

        log.info("✅ Отклонен отклик {} на проект {}", applicationId, application.getProjectId());
        return savedApplication;
    }

    @Transactional
    public void withdrawApplication(Long applicationId, Long freelancerChatId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Отклик не найден"));

        if (!application.getFreelancerChatId().equals(freelancerChatId)) {
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
        Project project = projectService.getProjectById(application.getProjectId()).orElseThrow(() -> new RuntimeException("Проект не найден"));
        project.setApplicationsCount(Math.max(0, project.getApplicationsCount() - 1));
        projectService.updateProject(project);

        log.info("✅ Пользователь {} отозвал отклик {}", applicationId, freelancerChatId);
    }

    @Transactional
    private void rejectOtherApplications(Long projectId, Long acceptedApplicationId) {
        List<Application> otherApplications = applicationRepository.findByProjectIdAndStatusOrderByAppliedAtDesc(
                projectId, UserRole.ApplicationStatus.PENDING);

        for (Application application : otherApplications) {
            if (!application.getId().equals(acceptedApplicationId)) {
                application.setStatus(UserRole.ApplicationStatus.REJECTED);
                application.setReviewedAt(LocalDateTime.now());
                application.setCustomerComment("Проект уже назначен другому исполнителю");
                eventPublisher.publishEvent(new ApplicationStatusChangedEvent(getApplicationDtoById(application.getId()), UserRole.ApplicationStatus.REJECTED));

            }
        }
        applicationRepository.saveAll(otherApplications);
    }

    public long getActiveApplicationsCount(Long chatId) {
        return applicationRepository.countActiveApplicationsByFreelancer(chatId);
    }

    public Optional<Application> getApplicationById(Long applicationId) {
        // 🔥 ИСПОЛЬЗУЕМ СТАНДАРТНЫЙ МЕТОД БЕЗ JOIN
        return applicationRepository.findById(applicationId);
    }

    // 🔥 ПОЛУЧЕНИЕ ID ПРОЕКТА ПО ID ОТКЛИКА
    public Long getProjectIdByApplicationId(Long applicationId) {
        Application application = getApplicationById(applicationId)
                .orElseThrow(() -> new RuntimeException("Отклик не найден"));
        return application.getProjectId();
    }

    public List<Long> getUserApplicationIds(Long chatId) {
        List<Application> applications = applicationRepository.findByFreelancerChatId(chatId);
        return applications.stream()
                .map(Application::getId)
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getApplicationsByIds(List<Long> applicationIds) {
        if (applicationIds.isEmpty()) return Collections.emptyList();

        List<Application> applications = applicationRepository.findAllById(applicationIds);

        // 🔥 ПАКЕТНАЯ ЗАГРУЗКА ДАННЫХ
        Map<Long, ProjectDto> projects = loadProjectsForApplications(applications);
        Map<Long, UserDto> freelancers = loadFreelancersForApplications(applications);

        return applications.stream()
                .map(app -> ApplicationDto.fromEntity(app,
                        projects.get(app.getProjectId()),
                        freelancers.get(app.getFreelancerChatId())))
                .collect(Collectors.toList());
    }

    public List<ApplicationDto> getUserApplicationDTOs(Long chatId) {
        List<Long> applicationIds = getUserApplicationIds(chatId);
        return getApplicationsByIds(applicationIds);
    }

    public List<ApplicationDto> getProjectApplicationDTOs(Long projectId) {
        List<Application> applications = applicationRepository.findByProjectIdOrderByAppliedAtDesc(projectId);
        List<Long> applicationIds = applications.stream()
                .map(Application::getId)
                .collect(Collectors.toList());
        return getApplicationsByIds(applicationIds);
    }

    private Map<Long, ProjectDto> loadProjectsForApplications(List<Application> applications) {
        List<Long> projectIds = applications.stream()
                .map(Application::getProjectId)
                .distinct()
                .collect(Collectors.toList());

        if (projectIds.isEmpty()) return new HashMap<>();

        // 🔥 ИСПОЛЬЗУЕМ СУЩЕСТВУЮЩИЙ МЕТОД
        List<ProjectDto> projectDTOs = projectService.getProjectsByIds(projectIds);
        return projectDTOs.stream()
                .collect(Collectors.toMap(ProjectDto::getId, Function.identity()));
    }

    private Map<Long, UserDto> loadFreelancersForApplications(List<Application> applications) {
        List<Long> freelancerChatIds = applications.stream()
                .map(Application::getFreelancerChatId)
                .distinct()
                .collect(Collectors.toList());

        if (freelancerChatIds.isEmpty()) return new HashMap<>();

        // 🔥 ИСПОЛЬЗУЕМ СУЩЕСТВУЮЩИЙ МЕТОД
        List<UserDto> freelancerDTOs = userService.getUsersDtoByChatIds(freelancerChatIds);
        return freelancerDTOs.stream()
                .collect(Collectors.toMap(UserDto::getChatId, Function.identity()));
    }

    public List<Application> findAllApplicationsByIds(List<Long> applicationIds) {
        return applicationRepository.findAllById(applicationIds);
    }

    // Исправляем метод getProjectApplicationIds
    public List<Long> getProjectApplicationIds(Long projectId) {
        return applicationRepository.findByProjectId(projectId).stream()
                .map(Application::getId) // 🔥 ДОБАВЛЯЕМ getId()
                .collect(Collectors.toList());
    }

    public ApplicationDto getApplicationDtoById(Long applicationId) {
        Application application = getApplicationById(applicationId)
                .orElseThrow(() -> new RuntimeException("Отклик не найден"));

        // 🔥 ЗАГРУЖАЕМ ДАННЫЕ ДЛЯ DTO
        ProjectDto project = projectService.getProjectDtoById(application.getProjectId()).orElse(null);
        UserDto freelancer = userService.getUserDtoByChatId(application.getFreelancerChatId()).orElse(null);

        return ApplicationDto.fromEntity(application, project, freelancer);
    }

    // 🔥 НОВЫЙ МЕТОД: Получение откликов фрилансера
    public List<Application> getApplicationsByFreelancerChatId(Long freelancerChatId) {
        // Используем findByFreelancerChatId из ApplicationRepository.java
        return applicationRepository.findByFreelancerChatId(freelancerChatId);
    }

    // 🔥 НОВЫЙ МЕТОД: Получение откликов по списку ID проектов
    public List<Application> getApplicationsByProjectIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        // Используем findByProjectIdIn из ApplicationRepository.java
        return applicationRepository.findByProjectIdIn(projectIds);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationDtosByIds(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Пакетная загрузка сущностей Application (по ID текущей страницы)
        // Предполагаем, что applicationRepository.findAllById(applicationIds) существует
        List<Application> applications = applicationRepository.findAllById(applicationIds);

        // 2. Сбор ID для пакетной загрузки связанных сущностей
        List<Long> projectIds = applications.stream()
                .map(Application::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> freelancerChatIds = applications.stream()
                .map(Application::getFreelancerChatId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3. Пакетная загрузка Project DTOs
        List<ProjectDto> projectDtos = projectService.getProjectsByIds(projectIds);
        Map<Long, ProjectDto> projectMap = projectDtos.stream()
                .collect(Collectors.toMap(ProjectDto::getId, Function.identity()));

        // 4. Пакетная загрузка Freelancer DTOs
        // userService.getUsersDtoByChatIds должен загружать пользователей по chat_id и возвращать UserDto
        List<UserDto> freelancerDtos = userService.getUsersDtoByChatIds(freelancerChatIds);
        Map<Long, UserDto> freelancerMap = freelancerDtos.stream()
                .collect(Collectors.toMap(UserDto::getChatId, Function.identity()));


        // 5. Конвертация Application -> ApplicationDto
        return applications.stream()
                .map(application -> {
                    ProjectDto project = projectMap.get(application.getProjectId());
                    UserDto freelancer = freelancerMap.get(application.getFreelancerChatId());

                    // 🔥 Вызов статического конвертера DTO
                    return ApplicationDto.fromEntity(application, project, freelancer);
                })
                .filter(Objects::nonNull) // Отфильтровать, если не удалось найти Project/Freelancer
                .collect(Collectors.toList());
    }

    public void notifyFreelancersAboutProjectCancellation(ProjectDto project) {
        try {
            // 🔥 ПОЛУЧАЕМ ВСЕ ОТКЛИКИ НА ПРОЕКТ
            List<Application> applications = applicationRepository.findByProjectId(project.getId());

            for (Application application : applications) {
                // 🔥 ОТМЕЧАЕМ ОТКЛИКИ КАК ОТКЛОНЕННЫЕ ИЗ-ЗА ОТМЕНЫ ПРОЕКТА
                if (application.getStatus() == UserRole.ApplicationStatus.PENDING) {
                    application.setStatus(UserRole.ApplicationStatus.REJECTED);
                    application.setReviewedAt(LocalDateTime.now());
                    application.setCoverLetter("Проект отменен заказчиком");
                    applicationRepository.save(application);
                    eventPublisher.publishEvent(new ApplicationStatusChangedEvent(getApplicationDtoById(application.getId()), UserRole.ApplicationStatus.REJECTED));
                }

//                // 🔥 ОТПРАВЛЯЕМ УВЕДОМЛЕНИЕ ИСПОЛНИТЕЛЮ
//                // (реализуйте логику отправки уведомлений через ваш BotExecutor)
//                String notificationText = """
//                <b>🔴 ПРОЕКТ ОТМЕНЕН</b>
//
//                <i>Заказчик отменил проект, на который вы откликнулись:</i>
//
//                <b>🎯 Проект:</b> %s
//                <b>💰 Бюджет:</b> %.0f руб
//
//                <i>💡 Ваш отклик был автоматически отклонен</i>
//                """.formatted(
//                        project.getTitle(),
//                        project.getBudget()
//                );
            }

            log.info("✅ Уведомления об отмене проекта {} отправлены {} исполнителям",
                    project.getId(), applications.size());

        } catch (Exception e) {
            log.error("❌ Ошибка отправки уведомлений об отмене проекта {}: {}", project.getId(), e.getMessage());
        }
    }

    /**
     * Ищет отклики со статусом ACCEPTED для данного фрилансера с пагинацией.
     */
    public List<Application> getApplicationsByFreelancerChatIdAndStatus(Long freelancerChatId, UserRole.ApplicationStatus status) {
        // 🔥 Предполагаем, что этот метод существует в вашем ApplicationRepository:
        // findByFreelancerChatIdAndStatus(Long chatId, ApplicationStatus status)
        return applicationRepository.findByFreelancerChatIdAndStatus(freelancerChatId, status);
    }
}
