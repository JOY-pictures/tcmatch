//package com.tcmatch.tcmatch.service;
//
//import com.tcmatch.tcmatch.bot.handlers.ApplicationHandler;
//import com.tcmatch.tcmatch.bot.handlers.ProjectsHandler;
//import jdk.jfr.Label;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Service;
//
//@Service
//@Slf4j
//public class TextRouterService {
//    private final UserSessionService userSessionService;
//    // 🔥 Зависимости от хендлеров, которые ожидают текстовый ввод:
//    private final ApplicationHandler applicationHandler;
//    private final ProjectsHandler projectHandler;
//    private final ProjectCreationService projectCreationService;
//
//    public TextRouterService(@Lazy UserSessionService userSessionService, @Lazy ApplicationHandler applicationHandler, ProjectsHandler projectHandler, ProjectService projectService, ProjectCreationService projectCreationService) {
//        this.userSessionService = userSessionService;
//        this.applicationHandler = applicationHandler;
//        this.projectHandler = projectHandler;
//        this.projectCreationService = projectCreationService;
//    }
//
//    // -----------------------------------------------------------------
//    // 🔥 ГЛАВНЫЙ МЕТОД: Маршрутизация текстового сообщения
//    // -----------------------------------------------------------------
//
////    public void routeTextMessage(Long chatId, String text, Integer messageId) {
////
////        // 1. Проверяем, идет ли процесс создания ОТКЛИКА
////        if (applicationHandler.isCreatingApplication(chatId)) {
////            applicationHandler.handleTextMessage(chatId, text, messageId);
////            return;
////        }
////
////        // 🔥 ПРОВЕРЯЕМ СОЗДАНИЕ ПРОЕКТА ПЕРВЫМ
////        if (projectCreationService.isCreatingProject(chatId)) {
////            projectHandler.handleProjectCreationTextMessage(chatId, text, messageId);
////            return;
////        }
////    }
//}
