package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {

    private static final String MESSAGE_AVAILABLE_COMMANDS = "Доступные команды:\n";
    private static final String MESSAGE_START_COMMAND = "/start - запустить бота;\n";
    private static final String MESSAGE_HELP_COMMAND = "/help - список доступных команд;\n";
    private static final String MESSAGE_TRACK_COMMAND = "/track - добавить ссылку;\n";
    private static final String MESSAGE_UNTRACK_COMMAND = "/untrack - удалить ссылку;\n";
    private static final String MESSAGE_LIST_COMMAND = "/list - список отслеживаемых ссылок;\n";
    private static final String MESSAGE_CANCEL_COMMAND = "/cancel - отменить текущую операцию.";

    private final TelegramBot telegramBot;

    @Override
    public String name() {
        return "/help";
    }

    @Override
    public void handle(Update update, String argument) {
        var chatId = update.message().chat().id();

        log.info("The /help command has been sent to the chat with chatId={}.", chatId);

        try {
            telegramBot.execute(new SendMessage(
                    chatId,
                    MESSAGE_AVAILABLE_COMMANDS
                            + MESSAGE_START_COMMAND
                            + MESSAGE_HELP_COMMAND
                            + MESSAGE_TRACK_COMMAND
                            + MESSAGE_UNTRACK_COMMAND
                            + MESSAGE_LIST_COMMAND
                            + MESSAGE_CANCEL_COMMAND));
        } catch (Exception exception) {
            log.error("Failed to send message. Command /help.", exception);
        }
    }
}
