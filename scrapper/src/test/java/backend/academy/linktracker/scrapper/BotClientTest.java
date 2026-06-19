package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.ResilientHttpExecutor;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.BotProperties;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class BotClientTest {

    private RestTemplate restTemplate;
    private BotProperties properties;
    private ResilientHttpExecutor resilientHttpExecutor;
    private ScrapperMetricsService metricsService;
    private BotClient botClient;

    @BeforeEach
    void setup() {
        restTemplate = mock(RestTemplate.class);
        resilientHttpExecutor = mock(ResilientHttpExecutor.class);
        metricsService = mock(ScrapperMetricsService.class);

        mockMetricsService(metricsService);

        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                })
                .when(resilientHttpExecutor)
                .executeVoid(any(Runnable.class));

        properties = new BotProperties();
        properties.setBaseUrl("http://localhost:8080");

        botClient = new BotClient(restTemplate, properties, resilientHttpExecutor, metricsService);
    }

    @Test
    void sendUpdateShouldCallRestTemplateThroughResilientExecutor() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "desc", "octocat", List.of(1L, 2L));

        botClient.sendUpdate(update);

        verify(metricsService).recordRequestDuration(anyString(), anyString(), any(Runnable.class));
        verify(resilientHttpExecutor).executeVoid(any(Runnable.class));
        verify(restTemplate).postForObject(eq("http://localhost:8080/updates"), eq(update), eq(Void.class));
    }

    private void mockMetricsService(ScrapperMetricsService metricsService) {
        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(2);
                    runnable.run();
                    return null;
                })
                .when(metricsService)
                .recordRequestDuration(anyString(), anyString(), any(Runnable.class));
    }
}
