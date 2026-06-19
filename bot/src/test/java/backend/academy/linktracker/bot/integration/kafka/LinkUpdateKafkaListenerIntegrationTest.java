package backend.academy.linktracker.bot.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.bot.dto.Priority;
import backend.academy.linktracker.bot.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.bot.integration.AbstractPostgresKafkaIntegrationTest;
import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import com.pengrad.telegrambot.TelegramBot;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class LinkUpdateKafkaListenerIntegrationTest extends AbstractPostgresKafkaIntegrationTest {

    private static final String TOPIC = PROCESSED_TOPIC;
    private static final String DLT_TOPIC_NAME = DLT_TOPIC;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KafkaConnectionDetails kafkaConnectionDetails;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TelegramBot telegramBot;

    @MockitoBean
    private LinkUpdateNotificationService notificationService;

    @Test
    void validMessage_shouldCallNotificationService() {
        ProcessedLinkUpdate update =
                new ProcessedLinkUpdate(999L, "Manual Kafka to Bot test", List.of(805052108L), Priority.HIGH);

        sendJsonToKafka("999", update);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(notificationService).sendUpdate(update));
    }

    @Test
    void invalidMessage_shouldBeSentToDltWithoutCallingNotificationService() {
        ProcessedLinkUpdate invalidUpdate = new ProcessedLinkUpdate(1000L, "Invalid message", null, Priority.HIGH);

        sendJsonToKafka("1000", invalidUpdate);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<String> dltMessages = readStringMessagesFromDlt();
            assertThat(dltMessages).anyMatch(message -> message.contains("Invalid message"));
        });

        verify(notificationService, never()).sendUpdate(any());
    }

    @Test
    void processingError_shouldRetryAndThenSendToDlt() {
        ProcessedLinkUpdate update =
                new ProcessedLinkUpdate(1001L, "Business error test", List.of(805052108L), Priority.HIGH);

        doThrow(new RuntimeException("Telegram unavailable"))
                .when(notificationService)
                .sendUpdate(update);

        sendJsonToKafka("1001", update);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> verify(notificationService, times(3)).sendUpdate(update));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<String> dltMessages = readStringMessagesFromDlt();
            assertThat(dltMessages).anyMatch(message -> message.contains("Business error test"));
        });
    }

    @Test
    void brokenJson_shouldBeSentToDltWithoutCallingNotificationService() {
        sendRawStringToKafka(UUID.randomUUID().toString(), "{\"id\":999,\"description\":");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<String> dltMessages = readStringMessagesFromDlt();
            assertThat(dltMessages).isNotEmpty();
        });

        verify(notificationService, never()).sendUpdate(any());
    }

    @Test
    void validMessage_shouldBeStoredAsProcessedInDatabase() {
        String key = "processed-" + UUID.randomUUID();

        ProcessedLinkUpdate update =
                new ProcessedLinkUpdate(3000L, "Processed DB test", List.of(805052108L), Priority.HIGH);

        sendJsonToKafka(key, update);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(notificationService).sendUpdate(update));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Integer count = jdbcClient
                    .sql("""
                            SELECT COUNT(*)
                            FROM processed_kafka_messages
                            WHERE message_key = :messageKey
                            """)
                    .param("messageKey", key)
                    .query(Integer.class)
                    .single();

            assertThat(count).isEqualTo(1);
        });
    }

    private void sendJsonToKafka(String key, ProcessedLinkUpdate update) {
        try {
            String json = objectMapper.writeValueAsString(update);
            sendRawStringToKafka(key, json);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize processed link update", exception);
        }
    }

    private void sendRawStringToKafka(String key, String value) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TOPIC, key, value));
            producer.flush();
        }
    }

    private List<String> readStringMessagesFromDlt() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaConnectionDetails.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "dlt-test-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        List<String> messages = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(DLT_TOPIC_NAME));

            long deadline = System.currentTimeMillis() + 3000;

            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

                for (ConsumerRecord<String, String> record : records) {
                    messages.add(record.value());
                }

                if (!messages.isEmpty()) {
                    break;
                }
            }
        }

        return messages;
    }
}
