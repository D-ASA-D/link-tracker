package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.bot.exception.InvalidLinkUpdateException;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import backend.academy.linktracker.bot.service.ProcessedKafkaMessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LinkUpdateKafkaListener {

    private static final String SCRAPPER_ASYNC_API_SCOPE = "scrapper_async_api";
    private static final String KAFKA_NOTIFICATION_SCOPE_TYPE = "kafkaNotification";

    private final LinkUpdateNotificationService notificationService;
    private final ProcessedKafkaMessageService processedMessageService;
    private final BotMetricsService metricsService;

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${app.kafka.group-id}")
    public void listen(ConsumerRecord<String, ProcessedLinkUpdate> record) {
        ProcessedLinkUpdate update = record.value();

        validate(update);

        if (processedMessageService.isProcessed(record.topic(), record.partition(), record.offset())) {
            log.info(
                    "kafka_processed_update_duplicate_skipped topic={} partition={} offset={} key={} id={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    update.id());
            return;
        }

        log.info(
                "kafka_processed_update_received topic={} partition={} offset={} key={} id={} priority={} chats={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                update.id(),
                update.priority(),
                update.tgChatIds());

        metricsService.recordCommandDuration(
                SCRAPPER_ASYNC_API_SCOPE, KAFKA_NOTIFICATION_SCOPE_TYPE, () -> notificationService.sendUpdate(update));

        processedMessageService.markProcessed(record.topic(), record.partition(), record.offset(), record.key());
    }

    private void validate(ProcessedLinkUpdate update) {
        if (update == null) {
            throw new InvalidLinkUpdateException("Processed link update must not be null");
        }

        if (update.id() == null) {
            throw new InvalidLinkUpdateException("Processed link update id must not be null");
        }

        if (update.description() == null || update.description().isBlank()) {
            throw new InvalidLinkUpdateException("Processed link update description must not be blank");
        }

        List<Long> chatIds = update.tgChatIds();
        if (chatIds == null || chatIds.isEmpty()) {
            throw new InvalidLinkUpdateException("Processed link update tgChatIds must not be empty");
        }

        if (update.priority() == null) {
            throw new InvalidLinkUpdateException("Processed link update priority must not be null");
        }
    }
}
