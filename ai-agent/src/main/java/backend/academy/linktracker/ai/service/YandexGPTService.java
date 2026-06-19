package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai-agent.summarization", name = "mode", havingValue = "YANDEX_GPT")
public class YandexGPTService implements UpdateSummarizer {

    private static final String PREVIEW_MARKER = "Превью:";

    private final SummarizationGatewayService summarizationGatewayService;
    private final AiAgentProperties properties;

    @Override
    public String summarize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int previewMarkerIndex = text.indexOf(PREVIEW_MARKER);

        if (previewMarkerIndex < 0) {
            return summarizeWholeTextIfNeeded(text);
        }

        int previewStartIndex = previewMarkerIndex + PREVIEW_MARKER.length();

        String prefix = text.substring(0, previewStartIndex);
        String preview = text.substring(previewStartIndex).trim();

        if (preview.length() <= properties.getSummarization().getThreshold()) {
            return text;
        }

        String summarizedPreview = summarizationGatewayService.summarizationTextForYandexGPT(preview);

        return prefix + "\n" + summarizedPreview;
    }

    private String summarizeWholeTextIfNeeded(String text) {
        if (text.length() <= properties.getSummarization().getThreshold()) {
            return text;
        }

        return summarizationGatewayService.summarizationTextForYandexGPT(text);
    }
}
