package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.client.YandexGPTClient;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummarizationGatewayService {
    private final YandexGPTClient yandexGPTClient;
    private final AiAgentProperties properties;
    private static final String ELLIPSIS = "...";

    @Retry(name = "gptServiceRetry")
    @CircuitBreaker(name = "gptServiceCB", fallbackMethod = "summarizationTextForStub")
    public String summarizationTextForYandexGPT(String preview) {
        return yandexGPTClient.summarize(preview);
    }

    @SuppressWarnings("unused")
    private String summarizationTextForStub(String preview, Throwable exception) {
        log.warn(
                "yandex_gpt_summarization_failed fallback=stub textLength={}",
                preview == null ? 0 : preview.length(),
                exception);
        if (preview == null || preview.isBlank()) {
            return "";
        }

        long threshold = properties.getSummarization().getThreshold();

        if (preview.length() <= threshold) {
            return preview;
        }

        return preview.substring(0, Math.toIntExact(threshold)) + ELLIPSIS;
    }
}
