package backend.academy.linktracker.scrapper.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class StackoverflowClientTest {

    private RestTemplate restTemplate;
    private StackoverflowProperties properties;
    private ResilientHttpExecutor resilientHttpExecutor;
    private ScrapperMetricsService metricsService;
    private StackoverflowClient client;

    @BeforeEach
    void setup() {
        restTemplate = mock(RestTemplate.class);
        resilientHttpExecutor = mock(ResilientHttpExecutor.class);
        metricsService = mock(ScrapperMetricsService.class);
        mockMetricsService();

        when(resilientHttpExecutor.execute(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        properties = new StackoverflowProperties();
        properties.setBaseUrl("http://localhost:8090");
        properties.setKey("test-key");
        properties.setAccessToken("test-token");

        client = new StackoverflowClient(restTemplate, properties, resilientHttpExecutor, metricsService);
    }

    @Test
    void getLastUpdateShouldReturnLatestAnswerInfo() {
        Map<String, Object> question = Map.of("title", "How to use Liquibase?");

        Map<String, Object> answerOwner = Map.of("display_name", "Jon Skeet");
        Map<String, Object> answer =
                Map.of("creation_date", 1772964000L, "body", "Very long answer body", "owner", answerOwner);

        when(restTemplate.getForObject(contains("/questions/12345?site=stackoverflow"), eq(Map.class)))
                .thenReturn(Map.of("items", List.of(question)));

        when(restTemplate.getForObject(contains("/questions/12345/answers"), eq(Map.class)))
                .thenReturn(Map.of("items", List.of(answer)));

        when(restTemplate.getForObject(contains("/questions/12345/comments"), eq(Map.class)))
                .thenReturn(Map.of("items", List.of()));

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isPresent());

        LinkUpdateInfo info = result.orElseThrow();
        assertEquals("How to use Liquibase?", info.title());
        assertEquals("Jon Skeet", info.username());
        assertEquals(Instant.ofEpochSecond(1772964000L), info.eventTime());
        assertEquals(Instant.ofEpochSecond(1772964000L), info.createdAt());
        assertEquals("Very long answer body", info.preview());
        assertEquals("ANSWER", info.eventType());

        verify(resilientHttpExecutor, times(3)).execute(any());
    }

    @Test
    void getLastUpdateShouldReturnEmpty_whenResponseIsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLastUpdateShouldReturnEmpty_whenItemsMissing() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of());

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLastUpdateShouldReturnEmpty_whenItemsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of("items", List.of()));

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLastUpdateShouldThrow_onException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("stackoverflow unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> client.getLastUpdate("https://stackoverflow.com/questions/12345/example"));
    }

    @Test
    void extractQuestionIdShouldReturnQuestionId() {
        String questionId = client.extractQuestionId("https://stackoverflow.com/questions/12345/example");

        assertEquals("12345", questionId);
    }

    @Test
    void extractQuestionIdShouldThrow_onInvalidUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.extractQuestionId("https://stackoverflow.com/invalid/url"));
    }

    @Test
    void buildQuestionApiUrlShouldUseBaseUrlFromProperties() {
        String apiUrl = client.buildQuestionApiUrl("12345");

        assertEquals("http://localhost:8090/questions/12345?site=stackoverflow&filter=withbody&key=test-key", apiUrl);
    }

    private void mockMetricsService() {
        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
    }
}
