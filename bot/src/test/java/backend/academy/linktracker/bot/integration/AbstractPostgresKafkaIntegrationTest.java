package backend.academy.linktracker.bot.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractPostgresKafkaIntegrationTest {

    public static final String PROCESSED_TOPIC = "link.processed-updates-test";
    public static final String DLT_TOPIC = "link.processed-updates-test-dlt";
    public static final String GROUP_ID = "link-tracker-bot-test";

    private static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:17"))
            .withDatabaseName("linktracker")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    static void startContainers() {
        KAFKA.start();
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("app.kafka.topic", () -> PROCESSED_TOPIC);
        registry.add("app.kafka.dlt-topic", () -> DLT_TOPIC);
        registry.add("app.kafka.group-id", () -> GROUP_ID);

        registry.add("app.kafka.retry-max-attempts", () -> "3");
        registry.add("app.kafka.retry-backoff-ms", () -> "100");
        registry.add("app.kafka.consumer.enabled", () -> "true");

        registry.add("spring.kafka.consumer.group-id", () -> GROUP_ID);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "false");
        registry.add(
                "spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "backend.academy.linktracker.bot.dto.ProcessedLinkUpdate");
        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> "false");

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "file:../migrations/bot-master.xml");

        registry.add("app.telegram.url", () -> "http://localhost:8080/bot");
        registry.add("app.telegram.token", () -> "test-token");

        registry.add("scrapper.base-url", () -> "http://localhost:8081");
    }
}
