package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFilteringServiceTest {

    private AiAgentProperties properties;
    private UpdateFilteringService service;

    @BeforeEach
    void setUp() {
        properties = new AiAgentProperties();

        properties.getFilter().setStopWords(List.of("spam", "ads", "promo"));
        properties.getFilter().setExcludedAuthors(List.of("bot-user"));
        properties.getFilter().setMinLength(20);

        service = new UpdateFilteringService(properties);
    }

    @Test
    void shouldFilterUpdateByStopWord() {
        RawLinkUpdate update = new RawLinkUpdate(
                1L, "This update contains spam content and should be ignored", "normal-user", List.of(111L));

        assertFalse(service.shouldProcess(update));
    }

    @Test
    void shouldFilterUpdateByExcludedAuthor() {
        RawLinkUpdate update =
                new RawLinkUpdate(1L, "This update has enough length and no stop words", "bot-user", List.of(111L));

        assertFalse(service.shouldProcess(update));
    }

    @Test
    void shouldFilterUpdateByMinimalLength() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "too short", "normal-user", List.of(111L));

        assertFalse(service.shouldProcess(update));
    }

    @Test
    void shouldPassValidUpdate() {
        RawLinkUpdate update = new RawLinkUpdate(
                1L, "This update has enough useful content and should be processed", "normal-user", List.of(111L));

        assertTrue(service.shouldProcess(update));
    }
}
