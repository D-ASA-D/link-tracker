package backend.academy.linktracker.scrapper.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldApplyMigrationsAndCreateExpectedTables() throws Exception {
        Set<String> tables = new HashSet<>();

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                 select table_name
                 from information_schema.tables
                 where table_schema = 'public'
                 """)) {

            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }
        }

        assertThat(tables)
                .contains("databasechangelog", "databasechangeloglock")
                .contains("chats", "links", "subscriptions", "tags", "subscription_tags");
    }
}
