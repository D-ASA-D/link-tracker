package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentProcessingService {
    private final UpdateFilteringService updateFilteringService;
    private final UpdateSummarizer updateSummarizer;
    private final PrioritizationService prioritizationService;

    public Optional<ProcessedLinkUpdate> process(RawLinkUpdate update) {
        if (!isValid(update)) {
            log.warn("raw_update_invalid id={} reason=invalid_required_fields", update == null ? null : update.id());
            return Optional.empty();
        }

        if (!updateFilteringService.shouldProcess(update)) {
            log.info("raw_update_filtered id={} author={}", update.id(), update.author());
            return Optional.empty();
        }

        String processedDescription = updateSummarizer.summarize(update.description());
        Priority priority = prioritizationService.definePriority(update.description());

        return Optional.of(new ProcessedLinkUpdate(update.id(), processedDescription, update.tgChatIds(), priority));
    }

    private boolean isValid(RawLinkUpdate update) {
        return update != null
                && update.id() != null
                && update.description() != null
                && !update.description().isBlank()
                && update.tgChatIds() != null
                && !update.tgChatIds().isEmpty();
    }
}
