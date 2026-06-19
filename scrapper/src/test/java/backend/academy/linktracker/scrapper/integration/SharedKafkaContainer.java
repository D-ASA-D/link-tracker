package backend.academy.linktracker.scrapper.integration;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class SharedKafkaContainer {

    public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    private SharedKafkaContainer() {}

    public static KafkaContainer getInstance() {
        if (!KAFKA.isRunning()) {
            KAFKA.start();
        }

        return KAFKA;
    }
}
