package com.tcmatch.tcmatch.service;


import com.tcmatch.tcmatch.events.NewProjectEvent;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.model.dto.SearchRequest;
import com.tcmatch.tcmatch.model.dto.UserDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final UserService userService;

    @Transactional
    public Project createProject(Long customerChatId, String title, String description,
                                  Double budget, String requiredSkills,
                                  Integer estimateDays) {
        User customer = userService.findByChatId(customerChatId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        customerChatId = customer.getChatId();

        // Проверяем, что пользователь имеет право создавать проекты
        if (!userService.hasFullAccess(customerChatId)) {
            throw new RuntimeException("Для создания проектов необходимо завершить регистрацию");
        }

        Project project = Project.builder()
                .title(title)
                .description(description)
                .budget(budget)
                .customerChatId(customerChatId)
                .deadline(null)
                .requiredSkills(requiredSkills)
                .estimatedDays(estimateDays)
                .build();

        Project savedProject = projectRepository.save(project);

        eventPublisher.publishEvent(new NewProjectEvent(getProjectDtoById(project.getId()).orElseThrow(() -> new RuntimeException("Проект не найден")), project.getCustomerChatId()));

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
        // 🔥 ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
        return projectRepository.findByCustomerChatIdOrderByCreatedAtDesc(chatId);
    }

//    public List<ProjectDto> getUserProjectsDto(Long chatId) {
//        return emp<>;
//    }

    public List<Project> getFreelancerProjects(Long chatId) {
        // 🔥 ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
        return projectRepository.findByFreelancerChatIdOrderByCreatedAtDesc(chatId);
    }

    public Optional<Project> getProjectById(Long projectId) {
        // 🔥 ИСПОЛЬЗУЕМ СТАНДАРТНЫЙ МЕТОД
        return projectRepository.findById(projectId);
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

    @Transactional
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
    @Transactional
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

    @Transactional
    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    @Transactional
    public Project assignFreelancer(Long projectId, long freelancerChatId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Проект не найден"));

        User freelancer = userService.findByChatId(freelancerChatId).orElseThrow(()-> new RuntimeException("Исполнитель не найден"));

        freelancerChatId = freelancer.getChatId();

        project.setFreelancerChatId(freelancerChatId);
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

    @Transactional(readOnly = true)
    public List<Project> findAllProjectsByIds(List<Long> projectsIds) {
        return projectRepository.findAllById(projectsIds);
    }

    public List<Long> searchActiveProjectIds(SearchRequest searchRequest) {
        List<Project> projects = searchActiveProjects(searchRequest);
        return projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());
    }

    public List<Project> getFavoriteProjectsPage(Long chatId, int page, int pageSize) {
        // 1. Получить все ID избранных проектов из UserService
        List<Long> favoriteIds = userService.getFavoriteProjectIds(chatId);

        // 2. Определить диапазон ID для текущей страницы
        int start = page * pageSize;
        int end = Math.min(start + pageSize, favoriteIds.size());

        if (start >= end) {
            return Collections.emptyList(); // Страница пуста или не существует
        }
        List<Long> pageIds = favoriteIds.subList(start, end);

        // 3. Загрузить проекты по ID (предполагаем, что findAllById существует)
        List<Project> projects = findAllProjectsByIds(pageIds);

        return projects.stream()
                .filter(p -> p.getStatus() == UserRole.ProjectStatus.OPEN)
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public int getFavoriteProjectsCount(Long chatId) {
        // В идеале этот метод должен загружать все избранные ID и фильтровать
        // количество активных, но для простоты, пока используем общий размер.
        // Если требуется точный подсчет, придется загружать и фильтровать все проекты.
        return userService.getFavoriteProjectIds(chatId).size();
    }

    @Transactional(readOnly = true)
    public List<Project> searchActiveProjects(SearchRequest request) {
        if (request == null || request.isEmpty()) {
            UserRole.ProjectStatus status = UserRole.ProjectStatus.OPEN;
            return projectRepository.findAllByStatusOrderByCreatedAtDesc(status);
        }

        // 🔥 Создание спецификации (динамического запроса)
        Specification<Project> spec = (root, query, cb) ->
                cb.equal(root.get("status"), "OPEN"
        );

        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + request.getKeyword().toLowerCase() + "%")
            );
        }

        if (request.getMinBudget() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("budget"), request.getMinBudget())
            );
        }

        // Настраиваем сортировку
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        // Выполняем поиск
        return projectRepository.findAll(spec, sort);
    }

    // ProjectService.java - ДОБАВЛЯЕМ МЕТОДЫ ДЛЯ ID
    public List<Long> getFavoriteProjectIds(Long chatId) {
        // 1. Получаем все избранные ID (используем существующий метод)
        // (Этот метод загружает ID из сущности User)
        List<Long> allFavoriteIds = userService.getFavoriteProjectIds(chatId);

        if (allFavoriteIds.isEmpty()) {
            return List.of();
        }

        // 2. Загружаем сами сущности проектов по их ID
        List<Project> favoriteProjects = projectRepository.findByIdIn(allFavoriteIds);

        // 3. 🔥 ФИЛЬТРУЕМ на уровне сервиса по статусу OPEN
        return favoriteProjects.stream()
                .filter(project -> project.getStatus() == UserRole.ProjectStatus.OPEN)
                .map(Project::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> searchProjectIds(SearchRequest searchRequest) {
        List<Project> projects = projectRepository.findActiveProjectsByFilters(
                searchRequest.getKeyword(),
                searchRequest.getRequiredSkills(),
                searchRequest.getMinBudget()
        );
        return projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> getUserProjectIds(Long chatId) {
        List<Project> projects = projectRepository.findByCustomerChatId(chatId);
        return projects.stream()
                .map(Project::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProjectDto> getProjectDtoById(Long projectId) {
        return projectRepository.findById(projectId)
                .map(project -> {
                    // 🔥 Загружаем заказчика по ID
                    UserDto customer = userService.getUserDtoByChatId(project.getCustomerChatId()).orElse(null);
                    return ProjectDto.fromEntity(project, customer);
                });
    }

    @Transactional
    public List<ProjectDto> getProjectsByIds(List<Long> projectIds) {
        if (projectIds.isEmpty()) return Collections.emptyList();

        List<Project> projects = projectRepository.findAllById(projectIds);

        // 🔥 Пакетная загрузка заказчиков по их ID
        List<Long> customerIds = projects.stream()
                .map(Project::getCustomerChatId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDto> customers = userService.getUsersDtoByChatIds(customerIds)
                .stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));

        return projects.stream()
                .map(project -> ProjectDto.fromEntity(project, customers.get(project.getCustomerChatId())))
                .collect(Collectors.toList());
    }



    public Long getCustomerChatIdByProjectId(Long projectId) {
        // 🔥 Вариант 1: если есть метод getProjectById который возвращает ProjectDto
        Project project = getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));
        return project.getCustomerChatId();
    }

    public String getProjectTitleById(Long projectId) {
        ProjectDto project = getProjectDtoById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));
        return project.getTitle();
    }

    // 🔥 НОВЫЙ МЕТОД: Получение ID проектов, созданных заказчиком
    public List<Long> getProjectIdsByCustomerChatId(Long customerChatId) {
        // Используем findByCustomerChatIdOrderByCreatedAtDesc из ProjectRepository.java
        return projectRepository.findByCustomerChatIdOrderByCreatedAtDesc(customerChatId).stream()
                .map(Project::getId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelProject(Long projectId, Long customerChatId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Проект не найден"));

        // 🔥 ПРОВЕРКА ПРАВ
        if (!project.getCustomerChatId().equals(customerChatId)) {
            throw new RuntimeException("У вас нет прав для отмены этого проекта");
        }

        // 🔥 ПРОВЕРКА СТАТУСА
        if (!canCancelProject(project.getStatus())) {
            throw new RuntimeException("Нельзя отменить проект со статусом: " + project.getStatus());
        }

        try {
            // 🔥 ОБНОВЛЯЕМ СТАТУС ПРОЕКТА
            project.setStatus(UserRole.ProjectStatus.CANCELLED);
//            project.setUpdatedAt(LocalDateTime.now());

            projectRepository.save(project);

            log.info("✅ Проект {} отменен пользователем {}", projectId, customerChatId);

        } catch (Exception e) {
            log.error("❌ Ошибка отмены проекта {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Не удалось отменить проект: " + e.getMessage());
        }
    }

    private boolean canCancelProject(UserRole.ProjectStatus projectStatus) {
        // 🔥 ПРОЕКТ МОЖНО УДАЛИТЬ ТОЛЬКО В ОПРЕДЕЛЕННЫХ СТАТУСАХ
        return switch (projectStatus) {
            case OPEN -> true;
            case IN_PROGRESS, COMPLETED, CANCELLED, UNDER_REVIEW, DISPUTE -> false;
        };
    }
}