package com.tcmatch.tcmatch.util;

public class PaginationContextKeys {

    // Константы для Откликов (ApplicationHandler)
    public static final String FREELANCER_APPLICATIONS_CONTEXT_KEY = "freelancer_applications";
    public static final String PROJECT_APPLICATIONS_CONTEXT_KEY = "project_applications";

    // Константы для Проектов (ProjectHandler, ProjectSearchService)
    public static final String PROJECT_SEARCH_CONTEXT_KEY = "project_search";
    public static final String PROJECT_FAVORITES_CONTEXT_KEY = "favorites";
    public static final String MY_PROJECTS_CONTEXT_KEY = "my_projects";

    public static final String ACCEPTED_APPLICATIONS_CONTEXT_KEY = "accepted_applications"; // 🔥 Новый ключ

    // 🔥 Константы для Уведомлений
    public static final String NOTIFICATION_CENTER_CONTEXT_KEY = "notification_center";

    // === КОЛБЭКИ ДЕЙСТВИЙ (То, что НЕ ДОЛЖНО СОХРАНЯТЬСЯ) ===
    public static final String PREFIX_PAGINATION_NEXT = "next";
    public static final String PREFIX_PAGINATION_PREV = "prev";
    public static final String PREFIX_PAGINATION_CURRENT = "current"; // 🔥 НОВАЯ КОНСТАНТА ДЛЯ ПЕРЕРИСОВКИ ТЕКУЩЕЙ СТРАНИЦЫ

    public static final String CALLBACK_PROJECTS_FILTER_APPLY = "projects:filter:apply";
    public static final String PREFIX_ACTION_ACCEPT = "accept";
    public static final String PREFIX_ACTION_REJECT = "reject";
    public static final String PREFIX_ACTION_WITHDRAW = "withdraw"; // (из KeyboardFactory)
    public static final String PREFIX_ACTION_DELETE = "delete";

    // Главные меню (их можно исключить из стека, так как они ведут на главные экраны)
    public static final String PREFIX_MENU = "menu";

    public static final int APPLICATIONS_PER_PAGE = 3;
    public static final int PROJECTS_PER_PAGE = 3;
    public static final int NOTIFICATIONS_PER_PAGE = 5; // 🔥 НОВЫЙ РАЗМЕР СТРАНИЦЫ


    private PaginationContextKeys() {
        // Запрет создания экземпляров
    }
}
