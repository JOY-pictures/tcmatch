package com.tcmatch.tcmatch.bot.exceptions;

public class DescriptionTooLongException extends IllegalArgumentException {
    public DescriptionTooLongException(String message) {
        super(message);
    }
}
