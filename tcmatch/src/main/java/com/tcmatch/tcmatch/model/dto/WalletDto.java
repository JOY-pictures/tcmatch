package com.tcmatch.tcmatch.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * DTO для отображения информации о кошельке пользователю.
 */
@Data
@Builder
public class WalletDto {
    private BigDecimal balance;
    private BigDecimal frozenBalance;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Форматирует доступный баланс для вывода в чат.
     * @return Строка, например: "1 234.50 RUB"
     */
    public String getFormattedBalance() {
        return DECIMAL_FORMAT.format(balance) + " RUB";
    }

    /**
     * Форматирует замороженный баланс для вывода в чат.
     * @return Строка, например: "500.00 RUB"
     */
    public String getFormattedFrozenBalance() {
        return DECIMAL_FORMAT.format(frozenBalance) + " RUB";
    }
}