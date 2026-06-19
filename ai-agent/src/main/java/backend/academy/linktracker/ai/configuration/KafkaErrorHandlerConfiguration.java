package backend.academy.linktracker.ai.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaErrorHandlerConfiguration {

    @Bean
    public CommonErrorHandler commonErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "kafka_message_processing_failed topic={} partition={} offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception),
                new FixedBackOff(0L, 0L));

        errorHandler.addNotRetryableExceptions(Exception.class);

        return errorHandler;
    }
}
