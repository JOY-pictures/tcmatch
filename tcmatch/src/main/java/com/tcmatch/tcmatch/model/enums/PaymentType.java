package com.tcmatch.tcmatch.model.enums;

import lombok.Getter;

@Getter
public enum PaymentType {
    FULL("Полная оплата в конце"),

    MILESTONES("Поэтапная оплата");

    private final String displayName;

    PaymentType(String displayName) {
        this.displayName = displayName;
    }
}
