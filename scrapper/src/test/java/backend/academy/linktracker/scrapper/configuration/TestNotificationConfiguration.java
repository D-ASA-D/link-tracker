package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.outbox.NotificationOutboxRepository;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationProperties;
import backend.academy.linktracker.scrapper.service.HttpUpdateSender;
import backend.academy.linktracker.scrapper.service.KafkaUpdateSender;
import backend.academy.linktracker.scrapper.service.OutboxUpdateSender;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Import({HttpUpdateSender.class, KafkaUpdateSender.class, OutboxUpdateSender.class})
@EnableConfigurationProperties(KafkaNotificationProperties.class)
class TestNotificationConfiguration {

    @Bean
    BotClient botClient() {
        return Mockito.mock(BotClient.class);
    }

    @Bean
    KafkaTemplate<String, RawLinkUpdate> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @Bean
    NotificationOutboxRepository notificationOutboxRepository() {
        return Mockito.mock(NotificationOutboxRepository.class);
    }
}
