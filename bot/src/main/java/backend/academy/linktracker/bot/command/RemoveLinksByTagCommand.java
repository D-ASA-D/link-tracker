package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.DeleteLinksByTagResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@RequiredArgsConstructor
public class RemoveLinksByTagCommand implements Command {

    private final TelegramBot telegramBot;
    private final ScrapperClient scrapperClient;

    @Override
    public String name() {
        return "/untag_links";
    }

    @Override
    public void handle(Update update, String argument) {

        Long chatId = update.message().chat().id();

        if (argument == null || argument.isBlank()) {
            telegramBot.execute(new SendMessage(chatId, "Использование: /untag_links tagName"));
            return;
        }

        String[] parts = argument.trim().split("\\s+");

        if (parts.length != 1) {
            telegramBot.execute(new SendMessage(chatId, "Использование: /untag_links tagName"));
            return;
        }

        String tag = parts[0];

        try {

            DeleteLinksByTagResponse response = scrapperClient.removeLinksByTag(chatId, tag);

            telegramBot.execute(new SendMessage(
                    chatId, "Удалено ссылок с тегом \"" + response.tag() + "\": " + response.removedCount()));

        } catch (HttpClientErrorException.NotFound e) {

            telegramBot.execute(new SendMessage(chatId, "Нет ссылок с таким тегом"));

        } catch (Exception e) {

            telegramBot.execute(new SendMessage(chatId, "Ошибка сервера. Попробуйте позже."));
        }
    }
}
