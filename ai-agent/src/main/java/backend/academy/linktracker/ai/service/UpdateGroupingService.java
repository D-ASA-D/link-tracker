package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.kafka.ProcessedUpdateProducer;
import backend.academy.linktracker.ai.model.GroupedUpdateBuffer;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateGroupingService {

    private final AiAgentProperties properties;
    private final ProcessedUpdateProducer producer;
    private final Clock clock;

    private final Map<Long, GroupedUpdateBuffer> buffers = new ConcurrentHashMap<>();
    private final AtomicLong groupedUpdateIdSequence = new AtomicLong(1);

    public void add(ProcessedLinkUpdate update) {
        for (Long tgChatId : update.tgChatIds()) {
            buffers.compute(tgChatId, (ignored, currentBuffer) -> {
                if (currentBuffer == null) {
                    return new GroupedUpdateBuffer(tgChatId, Instant.now(clock), update);
                }

                currentBuffer.add(update);
                return currentBuffer;
            });
        }
    }

    @Scheduled(fixedDelayString = "${ai-agent.grouping.window-ms:30000}")
    public void flushReadyGroups() {
        Instant now = Instant.now(clock);
        Duration window = Duration.ofMillis(properties.getGrouping().getWindowMs());

        for (Map.Entry<Long, GroupedUpdateBuffer> entry : buffers.entrySet()) {
            Long tgChatId = entry.getKey();
            GroupedUpdateBuffer buffer = entry.getValue();

            if (Duration.between(buffer.createdAt(), now).compareTo(window) < 0) {
                continue;
            }

            boolean removed = buffers.remove(tgChatId, buffer);

            if (!removed) {
                continue;
            }

            ProcessedLinkUpdate groupedUpdate = group(buffer);
            producer.send(groupedUpdate);

            log.info(
                    "grouped_update_sent chatId={} updatesCount={} priority={}",
                    buffer.tgChatId(),
                    buffer.size(),
                    groupedUpdate.priority());
        }
    }

    private ProcessedLinkUpdate group(GroupedUpdateBuffer buffer) {
        List<ProcessedLinkUpdate> updates = buffer.snapshot();

        if (updates.size() == 1) {
            ProcessedLinkUpdate update = updates.getFirst();

            return new ProcessedLinkUpdate(
                    update.id(), update.description(), List.of(buffer.tgChatId()), update.priority());
        }

        String description = buildNumberedDescription(updates);
        Priority priority = maxPriority(updates);

        return new ProcessedLinkUpdate(updates.getFirst().id(), description, List.of(buffer.tgChatId()), priority);
    }

    private String buildNumberedDescription(List<ProcessedLinkUpdate> updates) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < updates.size(); i++) {
            builder.append(i + 1).append(". ").append(updates.get(i).description());

            if (i < updates.size() - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private Priority maxPriority(List<ProcessedLinkUpdate> updates) {
        return updates.stream()
                .map(ProcessedLinkUpdate::priority)
                .max(Comparator.comparingInt(Priority::ordinal))
                .orElse(Priority.MEDIUM);
    }
}
