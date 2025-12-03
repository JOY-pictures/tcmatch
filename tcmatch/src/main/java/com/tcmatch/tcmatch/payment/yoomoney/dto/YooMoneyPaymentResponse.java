package com.tcmatch.tcmatch.payment.yoomoney.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class YooMoneyPaymentResponse {
    private String id; // ID платежа
    private String status;
    private Confirmation confirmation;

    @Data
    @NoArgsConstructor
    public static class Confirmation {
        private String type;

        @JsonProperty("confirmation_url")
        private String confirmationUrl; // Ссылка, куда нужно редиректить пользователя
    }
}