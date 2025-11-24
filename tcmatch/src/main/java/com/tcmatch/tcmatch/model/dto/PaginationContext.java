package com.tcmatch.tcmatch.model.dto;

import java.util.Collections;
import java.util.List;

public record PaginationContext<T>(
    Long chatId,
    List<Long> entityIds,                 // Полный список объектов для пагинации (Project, Application, etc.)
    String contextKey,
    String entityType,
    int currentPage,
    int pageSize
) {
    public PaginationContext withNewMessageIds(List<Integer> newMessageIds) {
        return new PaginationContext(this.chatId, this.entityIds, this.contextKey,this.entityType,
                this.currentPage, this.pageSize);
    }

    public PaginationContext withNewPage(int newPage) {
        return new PaginationContext(this.chatId, this.entityIds, this.contextKey,this.entityType,
                newPage, this.pageSize);
    }

    // 🔥 ВАЖНЫЕ ХЕЛПЕР-МЕТОДЫ
    public List<Long> getPageIds() {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyList();
        }
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, entityIds.size());
        return entityIds.subList(start, end);
    }

    public int getTotalPages() {
        if (entityIds == null || pageSize == 0) return 0;
        return (int) Math.ceil((double) entityIds.size() / pageSize);
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    // 🔥 СТАТИЧЕСКИЕ МЕТОДЫ ДЛЯ СОЗДАНИЯ
    public static PaginationContext forProjects(Long chatId, List<Long> projectIds, String contextKey, int pageSize) {
        return new PaginationContext(chatId, projectIds, contextKey,"PROJECT", 0, pageSize);
    }

    public static PaginationContext forApplications(Long chatId, List<Long> applicationIds, String contextKey, int pageSize) {
        return new PaginationContext(chatId, applicationIds, contextKey, "APPLICATION", 0, pageSize);
    }
}
