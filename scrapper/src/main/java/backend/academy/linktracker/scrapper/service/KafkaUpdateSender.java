package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaUpdateSender implements UpdateSender {
    private static final String KAFKA_SCOPE = "kafka";

    private final KafkaTemplate<String, RawLinkUpdate> kafkaTemplate;
    private final KafkaNotificationProperties properties;
    private final ScrapperMetricsService metricsService;

    @Override
    public void send(RawLinkUpdate update) {
        String key = String.valueOf(update.id());

        metricsService.recordRequestDuration(
                KAFKA_SCOPE, properties.getTopic(), () -> kafkaTemplate.send(properties.getTopic(), key, update));

        log.info(
                "raw_update_sent topic={} key={} author={} chatsCount={}",
                properties.getTopic(),
                key,
                update.author(),
                update.tgChatIds().size());
    }
}
