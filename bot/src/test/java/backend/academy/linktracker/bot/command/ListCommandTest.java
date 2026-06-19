package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ListCommandTest {

    @Test
    void shouldReturnEmptyListMessage() {

        ScrapperClient scrapperClient = mock(ScrapperClient.class);
        TelegramBot bot = mock(TelegramBot.class);

        ListCommand command = new ListCommand(scrapperClient, bot);

        Update update = mockUpdate();

        when(scrapperClient.getLinks(1L)).thenReturn(new ListLinksResponse(List.of(), 0));

        command.handle(update, null);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(captor.capture());

        String text = captor.getValue().getParameters().get("text").toString();

        assertThat(text).isEqualTo("Нет ссылок.");
    }

    @Test
    void shouldReturnNonEmptyListMessage() {

        ScrapperClient scrapperClient = mock(ScrapperClient.class);
        TelegramBot bot = mock(TelegramBot.class);

        ListCommand command = new ListCommand(scrapperClient, bot);

        Update update = mockUpdate();

        LinkResponse link1 = new LinkResponse(1L, "https://example.com", List.of(), List.of());
        LinkResponse link2 = new LinkResponse(2L, "https://site.ru", List.of(), List.of());

        when(scrapperClient.getLinks(1L)).thenReturn(new ListLinksResponse(List.of(link1, link2), 2));

        command.handle(update, null);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(captor.capture());

        String text = captor.getValue().getParameters().get("text").toString();

        assertThat(text)
                .startsWith("Отслеживаемые ссылки:")
                .contains("https://example.com")
                .contains("https://site.ru");
    }

    private Update mockUpdate() {

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(1L);

        return update;
    }
}
