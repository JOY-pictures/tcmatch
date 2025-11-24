package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.Project;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowActiveProjectsCommand implements Command {

    private final ProjectService projectService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final BotExecutor botExecutor;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "active".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        Long chatId = context.getChatId();
        try {
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
            // 🔥 РЕАЛЬНАЯ ЛОГИКА - получение активных проектов пользователя
            List<Project> activeProjects = projectService.getFreelancerProjects(chatId)
                    .stream()
                    .filter(p -> p.getStatus() == UserRole.ProjectStatus.IN_PROGRESS)
                    .collect(Collectors.toList());

            if (activeProjects.isEmpty()) {
                String text = """
                    ⚙️ <b>**ВЫПОЛНЯЕМЫЕ ПРОЕКТЫ**</b>

                    📊 <i>Сейчас у вас нет активных проектов</i>

                    💡 *Как получить заказы:*
                    • Активно откликайтесь на проекты
                    • Следите за своим рейтингом
                    • Предлагайте конкурентные условия
                    """;
                botExecutor.editMessageWithHtml(chatId, messageId, text, commonKeyboards.createBackButton());
                return;
            }

            // Показываем активные проекты
            showActiveProjectsList(context, activeProjects);

        } catch (Exception e) {
            log.error("❌ Ошибка показа активных проектов: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(chatId, "Ошибка загрузки активных проектов", 5);
        }
    }

    private void showActiveProjectsList(CommandContext context, List<Project> activeProjects) {
        Long chatId = context.getChatId();
        Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);
        String text = "<b>🚧 Раздел 'Выполняемые' в разработке...</b>";
        botExecutor.editMessageWithHtml(chatId, messageId, text, commonKeyboards.createBackButton());
    }
}
