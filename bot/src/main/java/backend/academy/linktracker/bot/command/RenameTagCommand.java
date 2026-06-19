package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.RenameTagRequest;
import backend.academy.linktracker.bot.dto.RenameTagResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RenameTagCommand implements Command {

    private final TelegramBot telegramBot;
    private final ScrapperClient scrapperClient;

    @Override
    public String name() {
        return "/tag_rename";
    }

    @Override
    public void handle(Update update, String argument) {

        Long chatId = update.message().chat().id();

        if (argument == null || argument.isBlank()) {
            telegramBot.execute(new SendMessage(chatId, "Использование: /tag_rename oldTag newTag"));
            return;
        }

        String[] parts = argument.trim().split("\\s+");

        if (parts.length != 2) {
            telegramBot.execute(new SendMessage(chatId, "Использование: /tag_rename oldTag newTag"));
            return;
        }

        RenameTagResponse response = scrapperClient.renameTag(chatId, new RenameTagRequest(parts[0], parts[1]));

        telegramBot.execute(new SendMessage(
                chatId,
                "Тег \"" + response.oldName()
                        + "\" переименован в \""
                        + response.newName()
                        + "\". Обновлено ссылок: "
                        + response.updatedSubscriptions()));
    }
}
