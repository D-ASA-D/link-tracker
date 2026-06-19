package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.KafkaNotificationProperties;
import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import backend.academy.linktracker.scrapper.properties.OutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({NotificationProperties.class, KafkaNotificationProperties.class, OutboxProperties.class
})
public class NotificationConfiguration {}
