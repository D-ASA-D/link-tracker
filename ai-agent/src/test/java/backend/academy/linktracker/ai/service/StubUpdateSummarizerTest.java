package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StubUpdateSummarizerTest {

    private StubUpdateSummarizer summarizer;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().setThreshold(10);

        summarizer = new StubUpdateSummarizer(properties);
    }

    @Test
    void shouldSummarizeLongText() {
        String text = "abcdefghijklmnopqrstuvwxyz";

        String result = summarizer.summarize(text);

        assertNotEquals(text, result);
        assertEquals("abcdefghij...", result);
        assertTrue(result.length() < text.length());
    }

    @Test
    void shouldReturnShortTextWithoutChanges() {
        String text = "short";

        String result = summarizer.summarize(text);

        assertEquals(text, result);
    }

    @Test
    void shouldReturnEmptyStringWhenTextIsNull() {
        String result = summarizer.summarize(null);

        assertEquals("", result);
    }
}
