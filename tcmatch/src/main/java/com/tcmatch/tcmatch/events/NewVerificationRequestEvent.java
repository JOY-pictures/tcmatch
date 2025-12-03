package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.VerificationRequest;
import lombok.Getter;

@Getter
public class NewVerificationRequestEvent {
    private final VerificationRequest verificationRequest;

    public NewVerificationRequestEvent(VerificationRequest verificationRequest) {
        this.verificationRequest = verificationRequest;
    }
}