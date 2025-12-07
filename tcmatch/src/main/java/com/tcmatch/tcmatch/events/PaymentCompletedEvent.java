package com.tcmatch.tcmatch.events;

import com.tcmatch.tcmatch.model.enums.SubscriptionTier;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {
    private final Long chatId;
    private final String paymentId;
    private final boolean success;
    private final String message;
    private final Double amount;

    public PaymentCompletedEvent(Object source, Long chatId, String paymentId,
                                 boolean success,
                                 String message, Double amount) {
        super(source);
        this.chatId = chatId;
        this.paymentId = paymentId;
        this.success = success;
        this.message = message;
        this.amount = amount;
    }
}