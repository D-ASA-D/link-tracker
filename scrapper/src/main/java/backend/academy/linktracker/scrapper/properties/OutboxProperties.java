package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private long intervalMs = 5000;

    private int batchSize = 100;

    private int maxAttempts = 5;

    private long sendTimeoutMs = 10000;
}
