package com.tcmatch.tcmatch.bot.exceptions;

import java.math.BigDecimal;

/**
 * Исключение, выбрасываемое при недостатке средств на балансе
 */
public class InsufficientFundsException extends RuntimeException {

    private final BigDecimal requiredAmount;
    private final BigDecimal currentBalance;

    public InsufficientFundsException(String message) {
        super(message);
        this.requiredAmount = null;
        this.currentBalance = null;
    }

    public InsufficientFundsException(String message, BigDecimal requiredAmount, BigDecimal currentBalance) {
        super(message);
        this.requiredAmount = requiredAmount;
        this.currentBalance = currentBalance;
    }

    public InsufficientFundsException(BigDecimal requiredAmount, BigDecimal currentBalance) {
        super(String.format(
                "Недостаточно средств. Требуется: %s ₽, доступно: %s ₽",
                requiredAmount, currentBalance
        ));
        this.requiredAmount = requiredAmount;
        this.currentBalance = currentBalance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Проверяет, является ли исключение InsufficientFundsException
     */
    public static boolean isInsufficientFunds(Throwable throwable) {
        return throwable != null &&
                (throwable instanceof InsufficientFundsException ||
                        (throwable.getCause() != null && throwable.getCause() instanceof InsufficientFundsException));
    }

    /**
     * Получает InsufficientFundsException из цепочки исключений
     */
    public static InsufficientFundsException extract(Throwable throwable) {
        if (throwable instanceof InsufficientFundsException) {
            return (InsufficientFundsException) throwable;
        }

        if (throwable.getCause() != null && throwable.getCause() instanceof InsufficientFundsException) {
            return (InsufficientFundsException) throwable.getCause();
        }

        return null;
    }
}