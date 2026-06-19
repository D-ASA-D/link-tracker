package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiAgentProcessingServiceTest {

    private AiAgentProcessingService service;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();

        properties.getFilter().setStopWords(List.of("spam"));
        properties.getFilter().setExcludedAuthors(List.of("bot-user"));
        properties.getFilter().setMinLength(10);

        properties.getSummarization().setThreshold(20);

        properties.getPrioritization().setHighKeywords(List.of("critical", "urgent", "breaking", "security"));
        properties.getPrioritization().setLowKeywords(List.of("minor", "typo", "chore", "docs"));

        UpdateFilteringService filteringService = new UpdateFilteringService(properties);
        StubUpdateSummarizer summarizer = new StubUpdateSummarizer(properties);
        PrioritizationService prioritizationService = new PrioritizationService(properties);

        service = new AiAgentProcessingService(filteringService, summarizer, prioritizationService);
    }

    @Test
    void shouldReturnProcessedUpdateWhenRawUpdateIsValid() {
        RawLinkUpdate update =
                new RawLinkUpdate(1L, "This is a valid update description", "normal-user", List.of(111L, 222L));

        Optional<ProcessedLinkUpdate> result = service.process(update);

        assertTrue(result.isPresent());

        ProcessedLinkUpdate processed = result.orElseThrow();

        assertEquals(1L, processed.id());
        assertEquals(List.of(111L, 222L), processed.tgChatIds());
        assertEquals(Priority.MEDIUM, processed.priority());
        assertEquals("This is a valid upda...", processed.description());
    }

    @Test
    void shouldReturnProcessedUpdateWithHighPriorityWhenTextContainsHighKeyword() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "critical update description", "normal-user", List.of(111L));

        Optional<ProcessedLinkUpdate> result = service.process(update);

        assertTrue(result.isPresent());

        ProcessedLinkUpdate processed = result.orElseThrow();

        assertEquals(Priority.HIGH, processed.priority());
        assertEquals("critical update desc...", processed.description());
    }

    @Test
    void shouldReturnProcessedUpdateWithLowPriorityWhenTextContainsLowKeyword() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "fix typo in readme file", "normal-user", List.of(111L));

        Optional<ProcessedLinkUpdate> result = service.process(update);

        assertTrue(result.isPresent());

        ProcessedLinkUpdate processed = result.orElseThrow();

        assertEquals(Priority.LOW, processed.priority());
        assertEquals("fix typo in readme f...", processed.description());
    }

    @Test
    void shouldReturnEmptyWhenUpdateIsFiltered() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "spam content with enough length", "normal-user", List.of(111L));

        Optional<ProcessedLinkUpdate> result = service.process(update);

        assertTrue(result.isEmpty());
    }
}
