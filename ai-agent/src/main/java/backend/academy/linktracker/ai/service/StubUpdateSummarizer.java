package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai-agent.summarization", name = "mode", havingValue = "STUB", matchIfMissing = true)
public class StubUpdateSummarizer implements UpdateSummarizer {

    private static final String ELLIPSIS = "...";

    private final AiAgentProperties properties;

    @Override
    public String summarize(String text) {
        if (text == null) {
            return "";
        }

        long threshold = properties.getSummarization().getThreshold();

        if (text.length() <= threshold) {
            return text;
        }

        return text.substring(0, Math.toIntExact(threshold)) + ELLIPSIS;
    }
}
