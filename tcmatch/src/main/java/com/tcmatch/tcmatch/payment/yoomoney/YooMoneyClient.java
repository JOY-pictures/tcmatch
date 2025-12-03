package com.tcmatch.tcmatch.payment.yoomoney;

import com.tcmatch.tcmatch.payment.yoomoney.dto.YooMoneyPaymentResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface YooMoneyClient {

    /**
     * Отправляет запрос в ЮKassa на создание нового платежа.
     * * @param amount Сумма платежа.
     * @param description Описание для чека/платежа.
     * @param idempotenceKey Уникальный ключ идемпотентности.
     * @return Объект ответа, содержащий ID платежа и ссылку на оплату.
     */
    YooMoneyPaymentResponse createPayment(Double amount, String description, UUID idempotenceKey);
}