package com.tcmatch.tcmatch.bot.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CommandContext {
    private Long chatId;
    private String action;
    private String parameter;
    private Integer messageId;
    private String userName;
    private String actionType;
}
