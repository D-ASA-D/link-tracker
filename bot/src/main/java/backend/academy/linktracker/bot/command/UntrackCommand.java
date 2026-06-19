package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.SessionService;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UntrackCommand implements Command {

    private static final String MESSAGE_REMOVE_LINK = "Отправьте ссылку для удаления.";
    private final SessionService sessionService;
    private final TelegramBot telegramBot;

    @Override
    public String name() {
        return "/untrack";
    }

    @Override
    public void handle(Update update, String argument) {

        var chatId = update.message().chat().id();
        var session = sessionService.get(chatId);

        session.setState(UserState.WAITING_FOR_UNTRACK);

        log.info("Untrack command started. chatId={}", chatId);

        telegramBot.execute(new SendMessage(chatId, MESSAGE_REMOVE_LINK));
    }
}
