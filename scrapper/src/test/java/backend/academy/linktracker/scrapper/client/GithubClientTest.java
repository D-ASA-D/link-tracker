package backend.academy.linktracker.scrapper.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class GithubClientTest {

    private RestTemplate restTemplate;
    private GithubProperties properties;
    private ResilientHttpExecutor resilientHttpExecutor;
    private ScrapperMetricsService metricsService;
    private GithubClient client;

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

        properties = new GithubProperties();
        properties.setToken("token");
        properties.setBaseUrl("http://localhost:8080");

        client = new GithubClient(restTemplate, properties, resilientHttpExecutor, metricsService);
    }

    @Test
    void getLastUpdateShouldReturnIssueInfo() {
        Map<String, Object> user = Map.of("login", "octocat");
        Map<String, Object> issue = Map.of(
                "title", "Fix auth bug",
                "body", "Issue description text",
                "created_at", "2026-03-08T10:00:00Z",
                "user", user);

        when(restTemplate.getForObject(
                        eq("http://localhost:8080/repos/user/repo/issues?sort=created&direction=desc&per_page=1"),
                        eq(List.class)))
                .thenReturn(List.of(issue));

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isPresent());

        LinkUpdateInfo info = result.orElseThrow();
        assertEquals(Instant.parse("2026-03-08T10:00:00Z"), info.eventTime());
        assertEquals("Fix auth bug", info.title());
        assertEquals("octocat", info.username());
        assertEquals(Instant.parse("2026-03-08T10:00:00Z"), info.createdAt());
        assertEquals("Issue description text", info.preview());
        assertEquals("ISSUE", info.eventType());

        verify(resilientHttpExecutor).execute(any());
        verify(restTemplate)
                .getForObject(
                        eq("http://localhost:8080/repos/user/repo/issues?sort=created&direction=desc&per_page=1"),
                        eq(List.class));
    }

    @Test
    void getLastUpdateShouldReturnPrInfo_whenPullRequestFieldExists() {
        Map<String, Object> user = Map.of("login", "octocat");
        Map<String, Object> pr = Map.of(
                "title", "Add feature",
                "body", "PR description",
                "created_at", "2026-03-08T10:00:00Z",
                "user", user,
                "pull_request", Map.of("url", "http://example.com"));

        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(List.of(pr));

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isPresent());
        assertEquals("PR", result.orElseThrow().eventType());
        verify(resilientHttpExecutor).execute(any());
    }

    @Test
    void getLastUpdateShouldReturnEmpty_whenResponseIsNull() {
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(null);

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLastUpdateShouldReturnEmpty_whenResponseIsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(List.of());

        Optional<LinkUpdateInfo> result = client.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLastUpdateShouldThrow_onException() {
        when(restTemplate.getForObject(anyString(), eq(List.class)))
                .thenThrow(new RuntimeException("github unavailable"));

        assertThrows(IllegalStateException.class, () -> client.getLastUpdate("https://github.com/user/repo"));
    }

    @Test
    void convertToIssuesApiUrlShouldUseBaseUrlFromProperties() {
        String apiUrl = client.convertToIssuesApiUrl("https://github.com/user/repo");

        assertEquals("http://localhost:8080/repos/user/repo/issues?sort=created&direction=desc&per_page=1", apiUrl);
    }

    private void mockMetricsService() {
        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
    }
}
