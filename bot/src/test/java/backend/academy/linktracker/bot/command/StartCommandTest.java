package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class StartCommandTest {

    @Test
    void shouldSendWelcomeMessage() {
        TelegramBot telegramBot = mock(TelegramBot.class);
        ScrapperClient scrapperClient = mock(ScrapperClient.class);
        ;
        StartCommand command = new StartCommand(scrapperClient, telegramBot);
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
                .isEqualTo(
                        "Добро пожаловать в Link Tracker Dasad. Я бот который оптимизирует твою работу с уведомлениями. Используйте /help, чтобы посмотреть список доступных команд.");
    }
}
