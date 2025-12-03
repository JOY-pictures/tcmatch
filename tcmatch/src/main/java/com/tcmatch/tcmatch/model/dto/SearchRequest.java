package com.tcmatch.tcmatch.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Объект для передачи данных фильтра поиска проектов.
 * Хранится в сессии пользователя, пока идет процесс поиска.
 */
@Data // Включает геттеры, сеттеры, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest implements Serializable {

    // 1. ПОИСК ПО КЛЮЧЕВОМУ СЛОВУ (например, в заголовке или описании)
    private String keyword;

    // 2. ТРЕБОВАНИЯ К НАВЫКАМ (List<String> или List<SkillEnum>)
    private List<String> requiredSkills;

    // 3. БЮДЖЕТ (минимальное и максимальное значение)
    private Integer minBudget;

    // 5. ... любые другие фильтры

    // -----------------------------------------------------------
    // Хелпер: Проверка, пуст ли запрос (если все поля null/empty)
    // -----------------------------------------------------------
    public boolean isEmpty() {
        return (keyword == null || keyword.isEmpty()) &&
                (requiredSkills == null || requiredSkills.isEmpty()) &&
                minBudget == null;
    }

    // Хелпер: Создание пустого запроса
    public static SearchRequest empty() {
        return new SearchRequest(null, null, null);
    }
}
