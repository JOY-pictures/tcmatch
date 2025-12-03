package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import lombok.Getter;

@Getter
public class NewApplicationEvent {
    private final ApplicationDto applicationDto;

    public NewApplicationEvent(ApplicationDto applicationDto) {
        this.applicationDto = applicationDto;
    }
}