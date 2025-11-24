package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.dto.ProjectDto;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WithdrawProjectCommand implements Command {

    private final BotExecutor botExecutor;
    private final ProjectService projectService;
    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final ApplicationService applicationService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "withdraw".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

            Long projectId = Long.parseLong(context.getParameter());

            // 🔥 ПОЛУЧАЕМ ИНФОРМАЦИЮ О ПРОЕКТЕ ДЛЯ СООБЩЕНИЯ
            ProjectDto projectDto = projectService.getProjectDtoById(projectId)
                    .orElseThrow(() -> new RuntimeException("Проект не найден"));

            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ПРОЕКТА
            if (!projectDto.getCustomerChatId().equals(chatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ У вас нет доступа к этому проекту", 5);
                return;
            }

            // 🔥 ОТМЕНЯЕМ ПРОЕКТ (МЕНЯЕМ СТАТУС)
            projectService.cancelProject(projectId, chatId);

            applicationService.notifyFreelancersAboutProjectCancellation(projectDto);

            String successText = """
            <b>🔴 **ПРОЕКТ ОТМЕНЕН**</b>
            
            <blockquote>📋 <b>Проект:</b> %s
            💰 <b>Бюджет:</b> %.0f руб
            ⏱️ <b>Срок:</b> %d дней
            👀 <b>Просмотров:</b> %d
            📨 <b>Откликов:</b> %d</blockquote>
            
            <i>✅ Проект перемещен в архив
            📨 Исполнители уведомлены об отмене
            🔒 Проект больше не виден в поиске</i>
            
            <b>💡 Что дальше:</b>
            • Создайте новый проект с обновленными требованиями
            • Изучите раздел "Помощь" для улучшения проектов
            • Обратитесь в поддержку при необходимости
            
            <b>📊 Проект сохранен в истории ваших проектов</b>
            """.formatted(
                    escapeHtml(projectDto.getTitle()),
                    projectDto.getBudget(),
                    projectDto.getEstimatedDays(),
                    projectDto.getViewsCount() != null ? projectDto.getViewsCount() : 0,
                    projectDto.getApplicationsCount() != null ? projectDto.getApplicationsCount() : 0
            );

            botExecutor.editMessageWithHtml(chatId, messageId, successText,
                    commonKeyboards.createToMainMenuKeyboard());

            log.info("✅ Пользователь {} отменил проект {}", chatId, projectId);

        } catch (Exception e) {
            log.error("❌ Ошибка отмены проекта: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(),
                    "Ошибка отмены проекта: " + e.getMessage(), 5);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}