package backend.academy.linktracker.scrapper.integration.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.outbox.JdbcNotificationOutboxRepository;
import backend.academy.linktracker.scrapper.outbox.OutboxEvent;
import backend.academy.linktracker.scrapper.outbox.OutboxEventStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "app.notification.transport=OUTBOX",
            "app.outbox.batch-size=10",
            "app.outbox.max-attempts=2",
            "app.outbox.interval-ms=999999"
        })
@ActiveProfiles("test")
class JdbcNotificationOutboxRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcNotificationOutboxRepository repository;

    @Test
    void save_shouldCreateNewOutboxEventAndTakeItForProcessing() {
        RawLinkUpdate update =
                new RawLinkUpdate(1L, "Обновление по ссылке https://github.com/test/repo", "test", List.of(123L));

        repository.save(update);

        List<OutboxEvent> events = repository.takeNewBatchForProcessing(10);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().aggregateId()).isEqualTo(1L);
        assertThat(events.getFirst().status()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(events.getFirst().attempts()).isZero();
        assertThat(events.getFirst().payload()).contains("Обновление по ссылке https://github.com/test/repo");
    }

    @Test
    void markSent_shouldSetStatusSentAndSentAt() {
        repository.save(
                new RawLinkUpdate(1L, "Обновление по ссылке https://github.com/test/repo", "test", List.of(123L)));

        OutboxEvent event = repository.takeNewBatchForProcessing(10).getFirst();

        repository.markSent(event.id());

        List<OutboxEvent> events = repository.takeNewBatchForProcessing(10);

        assertThat(events).isEmpty();
    }

    @Test
    void markFailedAttempt_shouldReturnEventToNewBeforeLimit() {
        repository.save(
                new RawLinkUpdate(1L, "Обновление по ссылке https://github.com/test/repo", "test", List.of(123L)));

        OutboxEvent event = repository.takeNewBatchForProcessing(10).getFirst();

        repository.markFailedAttempt(event.id(), 2, "first error");

        List<OutboxEvent> events = repository.takeNewBatchForProcessing(10);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().attempts()).isEqualTo(1);
        assertThat(events.getFirst().status()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(events.getFirst().lastError()).isEqualTo("first error");
    }

    @Test
    void markFailedAttempt_shouldSetFailedAfterMaxAttempts() {
        repository.save(
                new RawLinkUpdate(1L, "Обновление по ссылке https://github.com/test/repo", "test", List.of(123L)));

        OutboxEvent event = repository.takeNewBatchForProcessing(10).getFirst();

        repository.markFailedAttempt(event.id(), 2, "first error");

        OutboxEvent retryEvent = repository.takeNewBatchForProcessing(10).getFirst();

        repository.markFailedAttempt(retryEvent.id(), 2, "second error");

        List<OutboxEvent> events = repository.takeNewBatchForProcessing(10);

        assertThat(events).isEmpty();
    }
}
