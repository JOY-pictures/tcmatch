package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.VerificationRequest;
import lombok.Getter;

@Getter
public class VerificationStatusChangedEvent {
    private final VerificationRequest verificationRequest;
    private final Long adminId;

    public VerificationStatusChangedEvent(VerificationRequest verificationRequest, Long adminId) {
        this.verificationRequest = verificationRequest;
        this.adminId = adminId;
    }
}