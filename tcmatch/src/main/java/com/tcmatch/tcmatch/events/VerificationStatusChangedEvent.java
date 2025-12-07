package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.VerificationRequest;
import lombok.Getter;

@Getter
public class VerificationStatusChangedEvent {
    private final VerificationRequest verificationRequest;
    private final Long adminChatId;

    public VerificationStatusChangedEvent(VerificationRequest verificationRequest, Long adminChatId) {
        this.verificationRequest = verificationRequest;
        this.adminChatId = adminChatId;
    }
}