package backend.academy.linktracker.scrapper.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.integration.AbstractPostgresKafkaIntegrationTest;
import backend.academy.linktracker.scrapper.outbox.JdbcNotificationOutboxRepository;
import backend.academy.linktracker.scrapper.outbox.OutboxEvent;
import backend.academy.linktracker.scrapper.outbox.OutboxPublisherService;
import backend.academy.linktracker.scrapper.service.UpdateSender;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "app.notification.transport=OUTBOX",
            "app.kafka.topic=link.raw-updates-scrapper-test",
            "app.kafka.dlt-topic=link.raw-updates-scrapper-test-dlt",
            "app.outbox.batch-size=10",
            "app.outbox.max-attempts=3",
            "app.outbox.send-timeout-ms=5000",
            "app.outbox.interval-ms=999999",
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
            "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false"
        })
@ActiveProfiles("test")
class ScrapperOutboxKafkaIntegrationTest extends AbstractPostgresKafkaIntegrationTest {

    private static final String TOPIC = "link.raw-updates-scrapper-test";

    @Autowired
    private UpdateSender updateSender;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private JdbcNotificationOutboxRepository outboxRepository;

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Test
    void outboxUpdateSender_shouldSaveEventAndPublishItToKafka() {
        try (KafkaConsumer<String, RawLinkUpdate> consumer = createConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            consumer.poll(Duration.ofSeconds(1));

            RawLinkUpdate update =
                    new RawLinkUpdate(777L, "Scrapper outbox Kafka test", "octocat", List.of(805052108L));

            updateSender.send(update);

            List<OutboxEvent> events = outboxRepository.takeNewBatchForProcessing(10);

            assertThat(events).hasSize(1);
            assertThat(events.getFirst().aggregateId()).isEqualTo(777L);
            assertThat(events.getFirst().payload()).contains("Scrapper outbox Kafka test");
            assertThat(events.getFirst().payload()).contains("octocat");

            outboxRepository.markFailedAttempt(events.getFirst().id(), 3, "return to NEW for publisher test");

            outboxPublisherService.publishNewEvents();

            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                RawLinkUpdate actual = pollUpdate(consumer);

                assertThat(actual).isNotNull();
                assertThat(actual.id()).isEqualTo(777L);
                assertThat(actual.description()).isEqualTo("Scrapper outbox Kafka test");
                assertThat(actual.author()).isEqualTo("octocat");
                assertThat(actual.tgChatIds()).containsExactly(805052108L);
            });
        }
    }

    private KafkaConsumer<String, RawLinkUpdate> createConsumer() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "scrapper-outbox-kafka-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "backend.academy.linktracker.scrapper.dto");
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, RawLinkUpdate.class.getName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new KafkaConsumer<>(props);
    }

    private RawLinkUpdate pollUpdate(KafkaConsumer<String, RawLinkUpdate> consumer) {
        ConsumerRecords<String, RawLinkUpdate> records = consumer.poll(Duration.ofMillis(500));

        for (ConsumerRecord<String, RawLinkUpdate> record : records) {
            if (record.value() != null && record.value().id().equals(777L)) {
                return record.value();
            }
        }

        return null;
    }

    @TestConfiguration
    static class RealKafkaTemplateTestConfiguration {

        @Bean
        @Primary
        ProducerFactory<String, RawLinkUpdate> testProducerFactory(KafkaConnectionDetails kafkaConnectionDetails) {
            Map<String, Object> props = new HashMap<>();

            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
            props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

            return new DefaultKafkaProducerFactory<>(props);
        }

        @Bean
        @Primary
        KafkaTemplate<String, RawLinkUpdate> testKafkaTemplate(
                ProducerFactory<String, RawLinkUpdate> testProducerFactory) {
            return new KafkaTemplate<>(testProducerFactory);
        }
    }
}
