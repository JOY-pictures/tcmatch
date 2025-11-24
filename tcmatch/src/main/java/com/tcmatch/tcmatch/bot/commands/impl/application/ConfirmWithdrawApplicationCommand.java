package com.tcmatch.tcmatch.bot.commands.impl.application;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.ApplicationKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.model.dto.ApplicationDto;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.ApplicationService;
import com.tcmatch.tcmatch.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConfirmWithdrawApplicationCommand implements Command {

    private final BotExecutor botExecutor;
    private final ApplicationService applicationService;
    private final ProjectService projectService;
    private final CommonKeyboards commonKeyboards;
    private final ApplicationKeyboards applicationKeyboards;
    @Override
    public boolean canHandle(String actionType, String action) {
        return "application".equals(actionType) && "confirm_withdraw".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        try {
            Long chatId = context.getChatId();
            Integer messageId = botExecutor.getOrCreateMainMessageId(chatId);

            Long applicationId = Long.parseLong(context.getParameter());

            // 🔥 ИСПОЛЬЗУЕМ DTO ВМЕСТО СУЩНОСТИ
            ApplicationDto applicationDto = applicationService.getApplicationDtoById(applicationId);

            // 🔥 ПРОВЕРЯЕМ, ЧТО ПОЛЬЗОВАТЕЛЬ - ВЛАДЕЛЕЦ ОТКЛИКА
            if (!applicationDto.getFreelancerChatId().equals(chatId)) {
                botExecutor.sendTemporaryErrorMessage(chatId, "❌ У вас нет доступа к этому отклику", 5);
                return;
            }

            // 🔥 ПРОВЕРЯЕМ, ЧТО ОТКЛИК МОЖНО ОТОЗВАТЬ
            if (applicationDto.getStatus() != UserRole.ApplicationStatus.PENDING) {
                botExecutor.sendTemporaryErrorMessage(chatId,
                        "❌ Нельзя отозвать отклик со статусом: " + getApplicationStatusDisplay(applicationDto.getStatus()), 5);
                return;
            }


            // 🔥 ПОЛУЧАЕМ ДАННЫЕ ПРОЕКТА ЧЕРЕЗ СЕРВИС
            String projectTitle = projectService.getProjectTitleById(applicationDto.getProjectId());

            String warningText = """
            <b>⚠️ **ПОДТВЕРЖДЕНИЕ ОТЗЫВА ОТКЛИКА**</b>
            
            <blockquote>📋 *Проект:* %s
            💰 *Ваш бюджет:* %.0f руб
            ⏱️ *Ваш срок:* %d дней
            📅 *Отправлен:* %s</blockquote>
            
            🔴<b> *Внимание! </b>После отзыва:*
            <i>• Отклик будет отмечен как отозванный
            • Заказчик больше не увидит ваш отклик
            • Вернуть отклик будет невозможно
            • Использованный отклик не вернется в лимит</i>
            
            ❓ <b>*Вы точно хотите отозвать этот отклик?*</b>
            """.formatted(
                    projectTitle,
                    applicationDto.getProposedBudget(),
                    applicationDto.getProposedDays(),
                    applicationDto.getAppliedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            );
            InlineKeyboardMarkup keyboard = applicationKeyboards.createWithdrawConfirmationKeyboard(applicationId);

            botExecutor.editMessageWithHtml(chatId, messageId, warningText, keyboard);

        } catch (Exception e) {
            log.error("❌ Ошибка подтверждения отзыва отклика: {}", e.getMessage());
            botExecutor.sendTemporaryErrorMessage(context.getChatId(), "Ошибка подтверждения отзыва", 5);
        }
    }

    private String getApplicationStatusDisplay(UserRole.ApplicationStatus applicationStatus) {
        return switch (applicationStatus) {
            case PENDING -> "Ожидает рассмотрения";
            case ACCEPTED -> "Принят заказчиком";
            case REJECTED -> "Отклонен заказчиком";
            case WITHDRAWN -> "Отозван исполнителем";
        };
    }
}
