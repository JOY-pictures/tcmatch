package com.tcmatch.tcmatch.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TextProcessingResult {

    // ID сообщений, которые нужно удалить, кроме текущего
    List<Integer> messageIdsToDelete;

    // Флаг, который говорит боту НЕ УДАЛЯТЬ текущее сообщение
    boolean isTooLongError;

    // Вспомогательные методы для чистоты кода
    public static TextProcessingResult success(List<Integer> idsToDelete) {
        return new TextProcessingResult(idsToDelete, false);
    }

    public static TextProcessingResult tooLongError(List<Integer> idsToDelete) {
        return new TextProcessingResult(idsToDelete, true);
    }
}
