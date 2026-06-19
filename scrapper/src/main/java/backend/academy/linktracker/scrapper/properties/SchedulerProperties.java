package backend.academy.linktracker.scrapper.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {

    private long interval;
    private int batchSize;
    private int threads;
    private Duration recheckAge;
}
