package com.tcmatch.tcmatch.bot.commands;

public interface Command {
    boolean canHandle(String actionType, String action);
    void execute(CommandContext context);
}
