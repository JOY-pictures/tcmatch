package com.tcmatch.tcmatch.model.dto;

import lombok.Data;

@Data
public class ProjectData {
    private Long chatId;
    private Integer messageId;
    private String userName;
    private String filter;
    private String action;
    private String parameter;
    private Long projectId;

    public ProjectData(Long chatId, Integer messageId, String userName) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.userName = userName;
    }

    public ProjectData(Long chatId, Integer messageId, String userName, String filter, String action, String parameter) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.userName = userName;
        this.filter = filter;
        this.action = action;
        this.parameter = parameter;
    }
}
