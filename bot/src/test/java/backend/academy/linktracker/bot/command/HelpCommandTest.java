package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class HelpCommandTest {

    @Test
    void shouldSendHelpMessage() {
        TelegramBot telegramBot = mock(TelegramBot.class);
        HelpCommand command = new HelpCommand(telegramBot);
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn((long) 1);

        command.handle(update, null);
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());

        assertThat(captor.getValue().getParameters().get("text"))
                .isEqualTo("Доступные команды:\n" + "/start - запустить бота;\n" + "/help - список доступных команд;\n"
                        + "/track - добавить ссылку;\n"
                        + "/untrack - удалить ссылку;\n" + "/list - список отслеживаемых ссылок;\n"
                        + "/cancel - отменить текущую операцию.");
    }
}
