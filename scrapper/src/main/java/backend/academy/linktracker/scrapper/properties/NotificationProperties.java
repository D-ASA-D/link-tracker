package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private NotificationTransport transport = NotificationTransport.KAFKA;
}
