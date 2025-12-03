package com.tcmatch.tcmatch.payment.yoomoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 🔥 ИГНОРИРУЕМ НЕИЗВЕСТНЫЕ ПОЛЯ
public class YooMoneyNotification {

    @JsonProperty("type")
    private String type;

    @JsonProperty("event")
    private String event;

    @JsonProperty("object")
    private PaymentObject object;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true) // 🔥 И здесь тоже
    public static class PaymentObject {

        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;

        @JsonProperty("paid")
        private Boolean paid;

        @JsonProperty("test")
        private Boolean test;

        @JsonProperty("created_at")
        private String createdAt; // 🔥 Добавили поле

        @JsonProperty("amount")
        private Amount amount;

        @JsonProperty("metadata")
        private Object metadata;

        @Data
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Amount {

            @JsonProperty("value")
            private String value;

            @JsonProperty("currency")
            private String currency;
        }
    }
}