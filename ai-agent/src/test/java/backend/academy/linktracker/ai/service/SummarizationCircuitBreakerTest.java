package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.ai.client.YandexGPTClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

@SpringBootTest(
        properties = {
            "ai-agent.summarization.threshold=10",
            "resilience4j.retry.retry-aspect-order=2",
            "resilience4j.retry.instances.gptServiceRetry.max-attempts=1",
            "resilience4j.retry.instances.gptServiceRetry.wait-duration=1ms",
            "resilience4j.retry.instances.gptServiceRetry.retry-exceptions[0]=org.springframework.web.client.ResourceAccessException",
            "resilience4j.circuitbreaker.circuit-breaker-aspect-order=1",
            "resilience4j.circuitbreaker.instances.gptServiceCB.sliding-window-type=COUNT_BASED",
            "resilience4j.circuitbreaker.instances.gptServiceCB.sliding-window-size=2",
            "resilience4j.circuitbreaker.instances.gptServiceCB.minimum-number-of-calls=2",
            "resilience4j.circuitbreaker.instances.gptServiceCB.failure-rate-threshold=50",
            "resilience4j.circuitbreaker.instances.gptServiceCB.wait-duration-in-open-state=60s",
            "resilience4j.circuitbreaker.instances.gptServiceCB.permitted-number-of-calls-in-half-open-state=1"
        })
class SummarizationCircuitBreakerTest {

    @Autowired
    private SummarizationGatewayService service;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private YandexGPTClient yandexGPTClient;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("gptServiceCB").reset();
        reset(yandexGPTClient);
    }

    @Test
    void shouldOpenCircuitBreakerAndUseFallbackWithoutCallingYandexGpt() {
        String text = "abcdefghijklmnopqrstuvwxyz";

        when(yandexGPTClient.summarize(text)).thenThrow(new ResourceAccessException("YandexGPT unavailable"));

        assertEquals("abcdefghij...", service.summarizationTextForYandexGPT(text));
        assertEquals("abcdefghij...", service.summarizationTextForYandexGPT(text));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("gptServiceCB");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        reset(yandexGPTClient);

        String result = service.summarizationTextForYandexGPT(text);

        assertEquals("abcdefghij...", result);
        verifyNoInteractions(yandexGPTClient);
    }
}
