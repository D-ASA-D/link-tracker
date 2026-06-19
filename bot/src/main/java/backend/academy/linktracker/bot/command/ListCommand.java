package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListCommand implements Command {

    private static final String MESSAGE_THERE_ARE_NO_LINKS = "Нет ссылок.";
    private static final String MESSAGE_TRACKED_LINKS = "Отслеживаемые ссылки:\n";
    private static final String MESSAGE_THE_CHAT_IS_NOT_REGISTERED = "Чат не зарегистрирован. Используйте /start";

    private final ScrapperClient scrapperClient;
    private final TelegramBot telegramBot;

    @Override
    public String name() {
        return "/list";
    }

    @Override
    public void handle(Update update, String tag) {

        Long chatId = update.message().chat().id();

        log.info("Requesting links list. chatId={}, tag={}", chatId, tag);

        try {

            ListLinksResponse response = scrapperClient.getLinks(chatId);

            var links = response.links();

            if (tag != null) {
                links = links.stream().filter(l -> l.tags().contains(tag)).toList();
            }

            if (links.isEmpty()) {
                telegramBot.execute(new SendMessage(chatId, MESSAGE_THERE_ARE_NO_LINKS));
                return;
            }

            log.debug("Links received. chatId={}, count={}", chatId, links.size());

            StringBuilder text = new StringBuilder(MESSAGE_TRACKED_LINKS);

            links.forEach(link -> text.append(link.url()).append("\n"));

            telegramBot.execute(new SendMessage(chatId, text.toString()));

        } catch (HttpClientErrorException.NotFound e) {

            log.warn("Chat not registered in scrapper. chatId={}", chatId);

            telegramBot.execute(new SendMessage(chatId, MESSAGE_THERE_ARE_NO_LINKS));
        }
    }
}
