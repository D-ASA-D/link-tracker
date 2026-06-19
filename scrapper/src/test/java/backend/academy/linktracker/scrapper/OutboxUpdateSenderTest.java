package backend.academy.linktracker.scrapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.outbox.NotificationOutboxRepository;
import backend.academy.linktracker.scrapper.service.OutboxUpdateSender;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxUpdateSenderTest {

    private final NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
    private final OutboxUpdateSender sender = new OutboxUpdateSender(repository);

    @Test
    void send_shouldSaveEventToOutbox() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "test update", "octocat", List.of(123L));

        sender.send(update);

        verify(repository).save(update);
    }
}
