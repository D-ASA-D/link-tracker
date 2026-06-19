package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrioritizationServiceTest {

    private PrioritizationService service;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getPrioritization().setHighKeywords(List.of("critical", "urgent", "breaking", "security"));
        properties.getPrioritization().setLowKeywords(List.of("minor", "typo", "chore", "docs"));

        service = new PrioritizationService(properties);
    }

    @Test
    void shouldReturnHighWhenTextContainsHighKeyword() {
        Priority priority = service.definePriority("critical bug fix");

        assertEquals(Priority.HIGH, priority);
    }

    @Test
    void shouldReturnMediumWhenTextDoesNotContainKeywords() {
        Priority priority = service.definePriority("regular update with new answer");

        assertEquals(Priority.MEDIUM, priority);
    }

    @Test
    void shouldReturnLowWhenTextContainsLowKeyword() {
        Priority priority = service.definePriority("fix typo in readme");

        assertEquals(Priority.LOW, priority);
    }

    @Test
    void shouldPreferHighWhenTextContainsHighAndLowKeywords() {
        Priority priority = service.definePriority("critical typo fix");

        assertEquals(Priority.HIGH, priority);
    }
}
