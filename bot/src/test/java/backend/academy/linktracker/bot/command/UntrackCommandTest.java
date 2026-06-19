package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.state.SessionService;
import backend.academy.linktracker.bot.state.UserSession;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UntrackCommandTest {

    @Test
    void shouldStartUntrackDialog() {

        SessionService sessionService = mock(SessionService.class);
        TelegramBot bot = mock(TelegramBot.class);

        UntrackCommand command = new UntrackCommand(sessionService, bot);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(1L);

        UserSession session = new UserSession();
        when(sessionService.get(1L)).thenReturn(session);

        command.handle(update, null);

        assertThat(session.getState()).isEqualTo(UserState.WAITING_FOR_UNTRACK);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();

        assertThat(sentMessage.getParameters().get("text")).isEqualTo("Отправьте ссылку для удаления.");
    }
}
