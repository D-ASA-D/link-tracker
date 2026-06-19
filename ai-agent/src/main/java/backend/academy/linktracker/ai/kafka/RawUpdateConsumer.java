package backend.academy.linktracker.ai.kafka;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.service.AiAgentProcessingService;
import backend.academy.linktracker.ai.service.UpdateGroupingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawUpdateConsumer {

    private final AiAgentProcessingService aiAgentProcessingService;
    private final UpdateGroupingService updateGroupingService;

    @KafkaListener(topics = "${ai-agent.kafka.raw-updates-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(RawLinkUpdate update) {
        if (update == null) {
            log.warn("raw_update_invalid reason=null_message");
            return;
        }

        log.info("raw_update_received id={} author={}", update.id(), update.author());

        aiAgentProcessingService
                .process(update)
                .ifPresentOrElse(
                        updateGroupingService::add,
                        () -> log.info("raw_update_filtered id={} author={}", update.id(), update.author()));
    }
}
