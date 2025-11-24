package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.dto.ProjectDto;
import lombok.Getter;

@Getter
public class NewProjectEvent {
    private final ProjectDto projectDto;
    private final Long creatorChatId;

    public NewProjectEvent(ProjectDto projectDto, Long creatorChatId) {
        this.projectDto = projectDto;
        this.creatorChatId = creatorChatId;
    }
}