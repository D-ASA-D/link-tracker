package backend.academy.linktracker.ai.kafka;

import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedUpdateProducer {

    private final KafkaTemplate<String, ProcessedLinkUpdate> kafkaTemplate;

    private final AiAgentProperties aiAgentProperties;

    public void send(ProcessedLinkUpdate update) {
        String key = String.valueOf(update.id());

        String topic = aiAgentProperties.getKafka().getProcessedUpdatesTopic();

        kafkaTemplate.send(topic, key, update);

        log.info(
                "processed_update_sent topic={} key={} priority={} chatsCount={}",
                topic,
                key,
                update.priority(),
                update.tgChatIds().size());
    }
}
