package com.tcmatch.tcmatch.bot.commands.impl.project;

import com.tcmatch.tcmatch.bot.BotExecutor;
import com.tcmatch.tcmatch.bot.commands.Command;
import com.tcmatch.tcmatch.bot.commands.CommandContext;
import com.tcmatch.tcmatch.bot.keyboards.CommonKeyboards;
import com.tcmatch.tcmatch.bot.keyboards.ProjectKeyboards;
import com.tcmatch.tcmatch.model.enums.UserRole;
import com.tcmatch.tcmatch.service.RoleBasedMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
@RequiredArgsConstructor
public class MyProjectsMenuCommand implements Command {

    private final CommonKeyboards commonKeyboards;
    private final ProjectKeyboards projectKeyboards;
    private final BotExecutor botExecutor;
    private final RoleBasedMenuService roleBasedMenuService;

    @Override
    public boolean canHandle(String actionType, String action) {
        return "project".equals(actionType) && "my_projects".equals(action);
    }

    @Override
    public void execute(CommandContext context) {
        UserRole userRole = roleBasedMenuService.getUserRole(context.getChatId());

        if (userRole == UserRole.CUSTOMER) {
            String text = """
                👔 <b>**МОИ ПРОЕКТЫ**</b>

                <i>Управление вашими проектами:</i>
                """;
            InlineKeyboardMarkup keyboard = projectKeyboards.createMyProjectsMenu();
            botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), text, keyboard);
        } else {
            String text = """
                👨‍💻 <b>**УПРАВЛЕНИЕ ЗАКАЗАМИ**</b>

                📊 <u>Этот раздел доступен только заказчикам</u>

                💡 <i>Для исполнителей доступны:
                • ⚙️ Выполняемые - ваши активные заказы
                • 📨 Откликнутые - проекты, куда вы откликнулись
                • 🔍 Поиск проектов - находите новые проекты</i>
                """;
            InlineKeyboardMarkup keyboard = commonKeyboards.createBackButton();
            botExecutor.editMessageWithHtml(context.getChatId(), context.getMessageId(), text, keyboard);
        }
    }
}
