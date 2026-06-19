package backend.academy.linktracker.bot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaNotificationProperties {

    private String topic = "link.processed-updates";

    private String dltTopic = "link.processed-updates-dlt";

    private String groupId = "link-tracker-bot";

    private int retryMaxAttempts = 3;

    private long retryBackoffMs = 1000;
}
