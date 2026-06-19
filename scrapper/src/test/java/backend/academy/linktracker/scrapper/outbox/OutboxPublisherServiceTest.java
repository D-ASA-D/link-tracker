package backend.academy.linktracker.scrapper.outbox;

import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.properties.OutboxProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxPublisherServiceTest {

    private final NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
    private final KafkaOutboxMessagePublisher publisher = mock(KafkaOutboxMessagePublisher.class);

    private final OutboxProperties properties = new OutboxProperties();

    private final OutboxTransactionService transactionService = new OutboxTransactionService(repository);

    private final OutboxPublisherService service =
            new OutboxPublisherService(publisher, transactionService, properties);

    @Test
    void publishNewEvents_shouldMarkEventAsSent_whenPublishSucceeded() {
        properties.setBatchSize(10);
        properties.setMaxAttempts(3);

        OutboxEvent event = event();

        when(repository.takeNewBatchForProcessing(10)).thenReturn(List.of(event));

        service.publishNewEvents();

        verify(publisher).publish(event);
        verify(repository).markSent(event.id());
        verify(repository, never()).markFailedAttempt(any(), anyInt(), any());
    }

    @Test
    void publishNewEvents_shouldMarkFailedAttempt_whenPublishFailed() {
        properties.setBatchSize(10);
        properties.setMaxAttempts(3);

        OutboxEvent event = event();

        when(repository.takeNewBatchForProcessing(10)).thenReturn(List.of(event));
        doThrow(new IllegalStateException("Kafka unavailable")).when(publisher).publish(event);

        service.publishNewEvents();

        verify(publisher).publish(event);
        verify(repository).markFailedAttempt(event.id(), 3, "Kafka unavailable");
        verify(repository, never()).markSent(any());
    }

    private OutboxEvent event() {
        Instant now = Instant.now();

        return new OutboxEvent(
                UUID.randomUUID(), 1L, "LINK_UPDATE", """
            {"id":1,"url":"https://github.com/test/repo","description":"test","tgChatIds":[123]}
            """, OutboxEventStatus.NEW, 0, null, now, now, null);
    }
}
