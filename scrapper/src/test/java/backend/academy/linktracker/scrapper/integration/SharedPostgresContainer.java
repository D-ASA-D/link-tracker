package backend.academy.linktracker.scrapper.integration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class SharedPostgresContainer {

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:17"))
            .withDatabaseName("linktracker")
            .withUsername("postgres")
            .withPassword("postgres");

    private SharedPostgresContainer() {}

    public static PostgreSQLContainer<?> getInstance() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }

        return POSTGRES;
    }
}
