package backend.academy.linktracker.scrapper.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.client.ResilientHttpExecutor;
import backend.academy.linktracker.scrapper.configuration.ClientConfiguration;
import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GithubClientIntegrationTest.TestConfig.class)
@EnableWireMock
@TestPropertySource(properties = {"app.github.token=test-token"})
class GithubClientIntegrationTest {

    @TestConfiguration
    @Import({GithubClient.class, ResilientHttpExecutor.class, ResilienceConfiguration.class, ClientConfiguration.class})
    static class TestConfig {

        @Bean
        GithubProperties githubProperties() {
            return new GithubProperties();
        }

        @Bean
        @SuppressWarnings({"unchecked", "rawtypes"})
        ScrapperMetricsService scrapperMetricsService() {
            ScrapperMetricsService metricsService = mock(ScrapperMetricsService.class);

            when(metricsService.recordRequestDuration(
                            anyString(), anyString(), org.mockito.ArgumentMatchers.any(Supplier.class)))
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
                    .recordRequestDuration(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Runnable.class));

            return metricsService;
        }
    }

    @Autowired
    private GithubClient githubClient;

    @Autowired
    private GithubProperties githubProperties;

    @Autowired
    private Environment environment;

    @BeforeEach
    void setupProperties() {
        githubProperties.setBaseUrl(environment.getProperty("wiremock.server.baseUrl"));
        githubProperties.setToken("test-token");
    }

    @Test
    void shouldReturnIssueInfoFromWireMock() {
        stubFor(get(urlEqualTo("/repos/user/repo/issues?sort=created&direction=desc&per_page=1"))
                .willReturn(okJson("""
                [
                  {
                    "title": "Test issue",
                    "body": "Issue body text",
                    "created_at": "2026-04-05T12:00:00Z",
                    "user": {
                      "login": "octocat"
                    }
                  }
                ]
                """)));

        Optional<LinkUpdateInfo> result = githubClient.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isPresent());

        LinkUpdateInfo info = result.orElseThrow();
        assertEquals("Test issue", info.title());
        assertEquals("octocat", info.username());
        assertEquals(Instant.parse("2026-04-05T12:00:00Z"), info.createdAt());
        assertEquals(Instant.parse("2026-04-05T12:00:00Z"), info.eventTime());
        assertEquals("Issue body text", info.preview());
        assertEquals("ISSUE", info.eventType());
    }

    @Test
    void shouldReturnPrInfoFromWireMock() {
        stubFor(get(urlEqualTo("/repos/user/repo/issues?sort=created&direction=desc&per_page=1"))
                .willReturn(okJson("""
                [
                  {
                    "title": "Test PR",
                    "body": "PR body text",
                    "created_at": "2026-04-05T12:00:00Z",
                    "user": {
                      "login": "octocat"
                    },
                    "pull_request": {
                      "url": "http://example.com/pr/1"
                    }
                  }
                ]
                """)));

        Optional<LinkUpdateInfo> result = githubClient.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isPresent());

        LinkUpdateInfo info = result.orElseThrow();
        assertEquals("Test PR", info.title());
        assertEquals("octocat", info.username());
        assertEquals("PR", info.eventType());
        assertEquals("PR body text", info.preview());
    }

    @Test
    void shouldReturnEmptyWhenResponseIsEmpty() {
        stubFor(get(urlEqualTo("/repos/user/repo/issues?sort=created&direction=desc&per_page=1"))
                .willReturn(okJson("[]")));

        Optional<LinkUpdateInfo> result = githubClient.getLastUpdate("https://github.com/user/repo");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenServerErrorOccurs() {
        stubFor(get(urlEqualTo("/repos/user/repo/issues?sort=created&direction=desc&per_page=1"))
                .willReturn(serverError()));

        assertThrows(IllegalStateException.class, () -> githubClient.getLastUpdate("https://github.com/user/repo"));
    }
}
