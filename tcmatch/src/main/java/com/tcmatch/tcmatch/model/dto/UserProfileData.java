package com.tcmatch.tcmatch.model.dto;

import lombok.Data;

@Data
public class UserProfileData {
    private Long chatId;
    private Integer messageId;
    private String userName;


    public UserProfileData(Long chatId, Integer messageId, String userName) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.userName = userName;
    }
}
