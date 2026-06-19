package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.exception.InvalidLinkUpdateException;
import backend.academy.linktracker.bot.properties.KafkaNotificationProperties;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfiguration {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaOperations, KafkaNotificationProperties properties) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(properties.getDltTopic(), record.partition()));

        long retryAttempts = Math.max(0, properties.getRetryMaxAttempts() - 1);
        FixedBackOff backOff = new FixedBackOff(properties.getRetryBackoffMs(), retryAttempts);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                DeserializationException.class, MessageConversionException.class, InvalidLinkUpdateException.class);

        errorHandler.setLogLevel(KafkaException.Level.WARN);

        return errorHandler;
    }
}
