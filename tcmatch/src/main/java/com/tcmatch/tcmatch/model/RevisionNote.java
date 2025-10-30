package com.tcmatch.tcmatch.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionNote {
    private String notes;                  //Комментарий
    private LocalDateTime createdAt;
    private Integer revisionNumber;        //Номер правки
    private String requestedBy;
    private String noteType;

    @Builder.Default
    private Boolean isResolved = false;     // Исправлена ли правка
    private LocalDateTime resolvedAt;       // Когда исправлена
    private String resolutionNotes;         // Ответ на уточнение
}
