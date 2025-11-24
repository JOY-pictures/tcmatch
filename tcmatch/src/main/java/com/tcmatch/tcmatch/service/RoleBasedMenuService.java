package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoleBasedMenuService {

    private final UserService userService;
    private final ProjectService projectService;

    // 🔥 ПРОВЕРКИ ДОСТУПА ПО РОЛЯМ
    public boolean canUserApplyToProjects(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole() == UserRole.FREELANCER;
    }

    public boolean canUserCreateProjects(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole() == UserRole.CUSTOMER;
    }

    public boolean isProjectOwner(Long chatId, Long projectCustomerId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole() == UserRole.CUSTOMER &&
                user.getId().equals(projectCustomerId);
    }

    public UserRole getUserRole(Long chatId) {
        User user = userService.findByChatId(chatId).orElseThrow();
        return user.getRole();
    }
}
