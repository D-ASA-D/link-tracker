package backend.academy.linktracker.scrapper.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;

public abstract class AbstractPostgresKafkaIntegrationTest extends AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        KafkaContainer kafka = SharedKafkaContainer.getInstance();

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
