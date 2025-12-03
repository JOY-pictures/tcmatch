package com.tcmatch.tcmatch.model.enums;

public enum VerificationType {
    GITHUB("GitHub профиль"),
    PORTFOLIO("Портфолио"),
    IDENTITY("Личность");

    private final String displayName;

    VerificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}