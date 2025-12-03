package com.tcmatch.tcmatch.bot.commands.impl.profile;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProfileKeyboards;
import com.tcmatch.tcmatch.model.User;
import com.tcmatch.tcmatch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;


@Component
@Slf4j
@RequiredArgsConstructor
public class StatisticsCommand implements Command {

    private final UserService userService;
    private final CommonKeyboards commonKeyboards;
    private final ProfileKeyboards profileKeyboards;
    private final BotExecutor botExecutor;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "user_profile".equals(actionType) && "statistics".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        User user = userService.findByChatId(context.getChatId()).orElseThrow();
        Map<String, Object> stats = userService.getUserStatistics(context.getChatId());

        String statsText = """
            📊<b> **ДЕТАЛЬНАЯ СТАТИСТИКА**</b>
            • Всего проектов: %d
            • Завершено: %d
            • Успешных: %d (%.1f%%)
            • В срок: %d (%.1f%%)
            • Текущих заказов: %d
            • Активных откликов: %d
            
            • <u>В системе с: %s</u>
            """.formatted(
                stats.get("totalProjects"),
                stats.get("completedProjects"),
                stats.get("successfulProjects"),
                user.getSuccessRate(),
                user.getOnTimeProjectsCount(),
                user.getTimelinessRate(),
                stats.get("activeOrders"),
                stats.get("activeApplications"),
                user.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), statsText, commonKeyboards.createBackButton());
    }
}