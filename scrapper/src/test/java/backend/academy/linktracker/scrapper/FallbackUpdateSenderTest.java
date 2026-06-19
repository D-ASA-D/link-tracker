package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.outbox.NotificationOutboxRepository;
import backend.academy.linktracker.scrapper.service.FallbackUpdateSender;
import java.util.List;
import org.junit.jupiter.api.Test;

class FallbackUpdateSenderTest {

    @Test
    void shouldSaveUpdateToOutboxWhenHttpTransportFails() {
        BotClient botClient = mock(BotClient.class);
        NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);

        FallbackUpdateSender sender = new FallbackUpdateSender(botClient, outboxRepository);

        RawLinkUpdate update = new RawLinkUpdate(1L, "description", "octocat", List.of(805052108L));

        doThrow(new IllegalStateException("Bot HTTP unavailable"))
                .when(botClient)
                .sendUpdate(update);

        assertDoesNotThrow(() -> sender.send(update));

        verify(botClient).sendUpdate(update);
        verify(outboxRepository).save(update);
    }

    @Test
    void shouldNotUseOutboxWhenHttpTransportSucceeds() {
        BotClient botClient = mock(BotClient.class);
        NotificationOutboxRepository outboxRepository = mock(NotificationOutboxRepository.class);

        FallbackUpdateSender sender = new FallbackUpdateSender(botClient, outboxRepository);

        RawLinkUpdate update = new RawLinkUpdate(1L, "description", "octocat", List.of(805052108L));

        sender.send(update);

        verify(botClient).sendUpdate(update);
        verifyNoInteractions(outboxRepository);
    }
}
