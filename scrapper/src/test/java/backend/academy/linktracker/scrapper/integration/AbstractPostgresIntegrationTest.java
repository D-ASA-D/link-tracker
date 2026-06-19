package backend.academy.linktracker.scrapper.integration;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgres = SharedPostgresContainer.getInstance();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> false);

        registry.add("app.bot.base-url", () -> "http://localhost:8080");
        registry.add("app.scheduler.interval", () -> "60000");
        registry.add("app.github.token", () -> "test-token");
        registry.add("app.stackoverflow.key", () -> "test-key");
        registry.add("app.stackoverflow.access-token", () -> "test-access-token");
        registry.add("app.github.base-url", () -> "https://api.github.com");
        registry.add("app.stackoverflow.base-url", () -> "https://api.stackexchange.com/2.3");
    }

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanupDatabase() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    notification_outbox,
                    subscription_tags,
                    subscriptions,
                    tags,
                    links,
                    chats
                RESTART IDENTITY CASCADE
                """);
    }
}
