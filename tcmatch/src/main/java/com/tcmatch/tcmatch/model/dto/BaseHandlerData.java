package com.tcmatch.tcmatch.model.dto;

import lombok.Data;

@Data
public class BaseHandlerData {
    private Long chatId;
    private Integer messageId;
    private String userName;


    public BaseHandlerData(Long chatId, Integer messageId, String userName) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.userName = userName;
    }
}
