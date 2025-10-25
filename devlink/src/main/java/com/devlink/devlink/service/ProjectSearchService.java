package com.devlink.devlink.service;


import com.devlink.devlink.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectSearchService {

    private final ProjectService projectService;

    // Храним состояние поиска для каждого пользователя
    private final Map<Long, SearchState> userSearchState = new ConcurrentHashMap<>();

    public static class SearchState {
        public List<Project> projects;
        public int currentPage;
        public int pageSize;
        public String currentFilter;
    }

    public SearchState getOrCreateSearchState(Long chatId, String filter) {
        SearchState state = userSearchState.get(chatId);

        if (state == null || !filter.equals(state.currentFilter)) {
            List<Project> projects = projectService.searchProjects(filter);
            state = new SearchState();
            state.currentPage = 0;
            state.pageSize = 5;
            state.currentFilter = filter;
            userSearchState.put(chatId, state);
        }

        return state;
    }

    public List<Project> getPageProjects(Long chatId, String filter) {
        SearchState state = getOrCreateSearchState(chatId, filter);
        int start = state.currentPage * state.pageSize;
        int end = Math.min(start + state.currentPage, state.projects.size());

        if (start >= state.projects.size()) {
            return new ArrayList<>();
        }
        return state.projects.subList(start,end);
    }

    public boolean hasNextPage(Long chatId) {
        SearchState state = userSearchState.get(chatId);
        if (state == null) return false;

        int nextPageStart = (state.currentPage + 1) * state.pageSize;
        return nextPageStart < state.projects.size();
    }

    public boolean hasPrevPage(Long chatId) {
        SearchState state = userSearchState.get(chatId);
        return state != null && state.currentPage > 0;
    }

    public void nextPage(Long chatId) {
        SearchState state = userSearchState.get(chatId);
        if (state != null && hasNextPage(chatId)) {
            state.currentPage++;
        }
    }

    public void prevPage(Long chatId) {
        SearchState state = userSearchState.get(chatId);
        if (state != null && hasPrevPage(chatId)) {
            state.currentPage--;
        }
    }

    public int getCurrentPage(Long chatId) {
        SearchState state = userSearchState.get(chatId);
        return state != null ? state.currentPage : 0;
    }

    public int getTotalPages(Long chatId) {
        SearchState state = userSearchState.get(chatId);
        if (state != null || state.projects.isEmpty()) return 0;
        return (int) Math.ceil((double) state.projects.size() / state.pageSize);
    }

    public void clearSearchState(Long chatId) {
        userSearchState.remove(chatId);
    }
}
