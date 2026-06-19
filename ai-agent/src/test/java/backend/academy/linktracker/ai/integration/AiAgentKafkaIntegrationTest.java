package backend.academy.linktracker.ai.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(
        properties = {
            "ai-agent.kafka.raw-updates-topic=link.raw-updates",
            "ai-agent.kafka.processed-updates-topic=link.processed-updates",
            "ai-agent.filter.stop-words[0]=spam",
            "ai-agent.filter.stop-words[1]=ads",
            "ai-agent.filter.stop-words[2]=promo",
            "ai-agent.filter.excluded-authors[0]=bot-user",
            "ai-agent.filter.min-length=20",
            "ai-agent.summarization.mode=STUB",
            "ai-agent.summarization.threshold=500",
            "ai-agent.prioritization.high-keywords[0]=critical",
            "ai-agent.prioritization.high-keywords[1]=urgent",
            "ai-agent.prioritization.high-keywords[2]=breaking",
            "ai-agent.prioritization.high-keywords[3]=security",
            "ai-agent.prioritization.low-keywords[0]=minor",
            "ai-agent.prioritization.low-keywords[1]=typo",
            "ai-agent.prioritization.low-keywords[2]=chore",
            "ai-agent.prioritization.low-keywords[3]=docs",
            "ai-agent.grouping.window-ms=100",
            "spring.kafka.consumer.group-id=link-tracker-ai-agent-test",
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
            "spring.kafka.consumer.properties.spring.json.use.type.headers=false",
            "spring.kafka.consumer.properties.spring.json.value.default.type=backend.academy.linktracker.ai.dto.RawLinkUpdate",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false"
        })
class AiAgentKafkaIntegrationTest {

    private static final String RAW_TOPIC = "link.raw-updates";
    private static final String PROCESSED_TOPIC = "link.processed-updates";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPublishProcessedUpdateToKafka() throws Exception {
        RawLinkUpdate rawUpdate = new RawLinkUpdate(
                12345L, "critical update description with enough length for processing", "normal-user", List.of(111L));

        kafkaTemplate.send(RAW_TOPIC, String.valueOf(rawUpdate.id()), rawUpdate);
        kafkaTemplate.flush();

        ProcessedLinkUpdate processed = readProcessedUpdate(12345L);

        assertNotNull(processed);
        assertEquals(12345L, processed.id());
        assertEquals("critical update description with enough length for processing", processed.description());
        assertEquals(List.of(111L), processed.tgChatIds());
        assertEquals(Priority.HIGH, processed.priority());
    }

    @Test
    void shouldNotPublishFilteredUpdate() throws Exception {
        RawLinkUpdate filteredUpdate =
                new RawLinkUpdate(999L, "spam content with enough length", "normal-user", List.of(111L));

        kafkaTemplate.send(RAW_TOPIC, String.valueOf(filteredUpdate.id()), filteredUpdate);
        kafkaTemplate.flush();

        assertNoProcessedUpdate(999L, Duration.ofSeconds(2));
    }

    private ProcessedLinkUpdate readProcessedUpdate(Long expectedId) throws Exception {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(PROCESSED_TOPIC));

            return pollExpectedRecord(consumer, expectedId, Duration.ofSeconds(10));
        }
    }

    private ProcessedLinkUpdate pollExpectedRecord(
            KafkaConsumer<String, String> consumer, Long expectedId, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

            for (ConsumerRecord<String, String> record : records) {
                ProcessedLinkUpdate update = objectMapper.readValue(record.value(), ProcessedLinkUpdate.class);

                if (expectedId.equals(update.id())) {
                    return update;
                }
            }
        }

        throw new AssertionError("No message with id=" + expectedId + " received from topic " + PROCESSED_TOPIC);
    }

    private void assertNoProcessedUpdate(Long unexpectedId, Duration timeout) throws Exception {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(PROCESSED_TOPIC));

            long deadline = System.currentTimeMillis() + timeout.toMillis();

            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));

                for (ConsumerRecord<String, String> record : records) {
                    ProcessedLinkUpdate update = objectMapper.readValue(record.value(), ProcessedLinkUpdate.class);

                    if (unexpectedId.equals(update.id())) {
                        throw new AssertionError("Filtered update was published: id=" + unexpectedId);
                    }
                }
            }
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "processed-updates-test-consumer-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        return new KafkaConsumer<>(properties);
    }
}
