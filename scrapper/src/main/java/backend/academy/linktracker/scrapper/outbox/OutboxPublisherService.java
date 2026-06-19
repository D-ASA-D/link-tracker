package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.scrapper.properties.OutboxProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.notification.transport}' == 'OUTBOX' || '${app.notification.transport}' == 'HTTP_WITH_KAFKA_FALLBACK'")
public class OutboxPublisherService {

    private final KafkaOutboxMessagePublisher publisher;
    private final OutboxTransactionService transactionService;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:5000}")
    public void publishNewEvents() {
        List<OutboxEvent> events = transactionService.takeNewBatch(properties.getBatchSize());

        if (events.isEmpty()) {
            return;
        }

        log.info("outbox_publish_started count={}", events.size());

        for (OutboxEvent event : events) {
            publishOne(event);
        }

        log.info("outbox_publish_finished count={}", events.size());
    }

    private void publishOne(OutboxEvent event) {
        try {
            publisher.publish(event);
            transactionService.markSent(event.id());

            log.info("outbox_event_sent eventId={} aggregateId={}", event.id(), event.aggregateId());
        } catch (Exception e) {
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();

            transactionService.markFailedAttempt(event.id(), properties.getMaxAttempts(), error);

            log.error("outbox_event_publish_failed eventId={} aggregateId={}", event.id(), event.aggregateId(), e);
        }
    }
}
