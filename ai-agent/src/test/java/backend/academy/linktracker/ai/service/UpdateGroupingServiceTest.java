package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.kafka.ProcessedUpdateProducer;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UpdateGroupingServiceTest {

    private ProcessedUpdateProducer producer;
    private UpdateGroupingService service;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getGrouping().setWindowMs(0);

        producer = Mockito.mock(ProcessedUpdateProducer.class);

        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        service = new UpdateGroupingService(properties, producer, clock);
    }

    @Test
    void shouldGroupMultipleUpdatesForSameChat() {
        ProcessedLinkUpdate first = new ProcessedLinkUpdate(1L, "critical bug fix", List.of(111L), Priority.HIGH);
        ProcessedLinkUpdate second = new ProcessedLinkUpdate(2L, "fix typo in docs", List.of(111L), Priority.LOW);

        service.add(first);
        service.add(second);

        service.flushReadyGroups();

        ArgumentCaptor<ProcessedLinkUpdate> captor = ArgumentCaptor.forClass(ProcessedLinkUpdate.class);
        verify(producer).send(captor.capture());

        ProcessedLinkUpdate result = captor.getValue();

        assertEquals(1L, result.id());
        assertEquals(List.of(111L), result.tgChatIds());
        assertEquals(Priority.HIGH, result.priority());
        assertTrue(result.description().contains("1. critical bug fix"));
        assertTrue(result.description().contains("2. fix typo in docs"));
    }

    @Test
    void shouldSendSingleUpdateWithoutGroupingChanges() {
        ProcessedLinkUpdate update = new ProcessedLinkUpdate(1L, "regular update", List.of(111L), Priority.MEDIUM);

        service.add(update);

        service.flushReadyGroups();

        ArgumentCaptor<ProcessedLinkUpdate> captor = ArgumentCaptor.forClass(ProcessedLinkUpdate.class);
        verify(producer).send(captor.capture());

        ProcessedLinkUpdate result = captor.getValue();

        assertEquals(1L, result.id());
        assertEquals("regular update", result.description());
        assertEquals(List.of(111L), result.tgChatIds());
        assertEquals(Priority.MEDIUM, result.priority());
    }
}
