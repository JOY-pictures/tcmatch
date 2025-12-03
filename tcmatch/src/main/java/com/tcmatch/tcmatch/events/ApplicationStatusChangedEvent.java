package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import lombok.Getter;

@Getter
public class ApplicationStatusChangedEvent {
    private final ApplicationDto applicationDto; // <-- ИЗМЕНЕНИЕ
    private final UserRole.ApplicationStatus newStatus;

    public ApplicationStatusChangedEvent(ApplicationDto applicationDto, UserRole.ApplicationStatus newStatus) { // <-- ИЗМЕНЕНИЕ
        this.applicationDto = applicationDto;
        this.newStatus = newStatus;
    }
}
