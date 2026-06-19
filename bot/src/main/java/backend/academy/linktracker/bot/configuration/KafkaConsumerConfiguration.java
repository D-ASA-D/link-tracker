package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.properties.KafkaNotificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaNotificationProperties.class)
public class KafkaConsumerConfiguration {}
