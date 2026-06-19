package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private static final String WELCOME_MESSAGE = "Добро пожаловать в Link Tracker Dasad. "
            + "Я бот который оптимизирует твою работу с уведомлениями. Используйте /help, чтобы посмотреть список доступных команд.";

    private final ScrapperClient scrapperClient;
    private final TelegramBot telegramBot;

    @Override
    public String name() {
        return "/start";
    }

    @Override
    public void handle(Update update, String argument) {

        Long chatId = update.message().chat().id();

        try {
            log.info("Executing /start command. chatId={}", chatId);
            scrapperClient.registerChat(chatId);
        } catch (Exception e) {
            log.info("Chat already registered chatId={}", chatId);
        }

        telegramBot.execute(new SendMessage(chatId, WELCOME_MESSAGE));

        log.info("Chat successfully activated. chatId={}", chatId);
    }
}
