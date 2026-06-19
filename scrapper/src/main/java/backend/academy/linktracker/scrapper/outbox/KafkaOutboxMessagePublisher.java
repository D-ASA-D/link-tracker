package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationProperties;
import backend.academy.linktracker.scrapper.properties.OutboxProperties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.notification.transport}' == 'OUTBOX' || '${app.notification.transport}' == 'HTTP_WITH_KAFKA_FALLBACK'")
public class KafkaOutboxMessagePublisher {

    private static final String KAFKA_SCOPE = "kafka";

    private final KafkaTemplate<String, RawLinkUpdate> kafkaTemplate;
    private final KafkaNotificationProperties kafkaProperties;
    private final OutboxProperties outboxProperties;
    private final ObjectMapper objectMapper;
    private final ScrapperMetricsService metricsService;

    public void publish(OutboxEvent event) {
        RawLinkUpdate update = toRawLinkUpdate(event.payload());
        String key = String.valueOf(update.id());

        metricsService.recordRequestDuration(
                KAFKA_SCOPE, kafkaProperties.getTopic(), () -> sendToKafka(event, update, key));
    }

    private void sendToKafka(OutboxEvent event, RawLinkUpdate update, String key) {
        try {
            kafkaTemplate
                    .send(kafkaProperties.getTopic(), key, update)
                    .get(outboxProperties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish was interrupted. eventId=" + event.id(), exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Failed to publish outbox event to Kafka. eventId=" + event.id(), exception);
        }
    }

    private RawLinkUpdate toRawLinkUpdate(String payload) {
        try {
            return objectMapper.readValue(payload, RawLinkUpdate.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize outbox payload", exception);
        }
    }
}
