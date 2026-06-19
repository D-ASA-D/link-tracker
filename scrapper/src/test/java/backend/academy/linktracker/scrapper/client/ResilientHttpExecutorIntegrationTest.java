package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.configuration.ClientConfiguration;
import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestTemplate;

class ResilientHttpExecutorIntegrationTest {

    private static final String GITHUB_URL = "https://github.com/test/repo";
    private static final String API_PATH = "/repos/test/repo/issues";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void shouldFailByTimeoutWhenExternalServiceRespondsTooLong() {
        ResilienceProperties properties = defaultProperties();
        properties.getTimeout().setReadTimeout(Duration.ofMillis(200));
        properties.getRetry().setMaxAttempts(1);

        GithubClient client = githubClient(properties);

        wireMock.stubFor(
                get(urlPathEqualTo(API_PATH)).willReturn(okJson(successBody()).withFixedDelay(1_500)));

        long startedAt = System.currentTimeMillis();

        assertThrows(IllegalStateException.class, () -> client.getLastUpdate(GITHUB_URL));

        long durationMs = System.currentTimeMillis() - startedAt;

        assertTrue(durationMs < 1_500);
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(API_PATH)));
    }

    @Test
    void shouldRetryRetryableStatusAndReturnSuccessfulResponse() {
        ResilienceProperties properties = defaultProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setWaitDuration(Duration.ofMillis(50));

        GithubClient client = githubClient(properties);

        wireMock.stubFor(get(urlPathEqualTo(API_PATH))
                .inScenario("retry-500-500-200")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(serverError())
                .willSetStateTo("second"));

        wireMock.stubFor(get(urlPathEqualTo(API_PATH))
                .inScenario("retry-500-500-200")
                .whenScenarioStateIs("second")
                .willReturn(serverError())
                .willSetStateTo("third"));

        wireMock.stubFor(get(urlPathEqualTo(API_PATH))
                .inScenario("retry-500-500-200")
                .whenScenarioStateIs("third")
                .willReturn(okJson(successBody())));

        Optional<LinkUpdateInfo> result = client.getLastUpdate(GITHUB_URL);

        assertTrue(result.isPresent());
        assertEquals("test issue", result.orElseThrow().title());

        wireMock.verify(3, getRequestedFor(urlPathEqualTo(API_PATH)));
    }

    @Test
    void shouldNotRetryNonRetryableStatus() {
        ResilienceProperties properties = defaultProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setRetryableStatuses(java.util.List.of(500, 502, 503, 504));

        GithubClient client = githubClient(properties);

        wireMock.stubFor(get(urlPathEqualTo(API_PATH)).willReturn(badRequest()));

        assertThrows(IllegalStateException.class, () -> client.getLastUpdate(GITHUB_URL));

        wireMock.verify(1, getRequestedFor(urlPathEqualTo(API_PATH)));
    }

    @Test
    void shouldRespectConstantBackoffBetweenRetries() {
        ResilienceProperties properties = defaultProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setWaitDuration(Duration.ofMillis(200));
        properties.getRetry().setBackoffStrategy(ResilienceProperties.BackoffStrategy.CONSTANT);

        GithubClient client = githubClient(properties);

        wireMock.stubFor(get(urlPathEqualTo(API_PATH)).willReturn(serverError()));

        long startedAt = System.currentTimeMillis();

        assertThrows(IllegalStateException.class, () -> client.getLastUpdate(GITHUB_URL));

        long durationMs = System.currentTimeMillis() - startedAt;

        assertTrue(durationMs >= 350);
        assertTrue(durationMs < 1_500);

        wireMock.verify(3, getRequestedFor(urlPathEqualTo(API_PATH)));
    }

    private GithubClient githubClient(ResilienceProperties properties) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        ResilienceConfiguration resilienceConfiguration = new ResilienceConfiguration();

        RestTemplate restTemplate = clientConfiguration.restTemplate(properties);

        ResilientHttpExecutor executor = new ResilientHttpExecutor(
                resilienceConfiguration.externalHttpRetry(properties),
                resilienceConfiguration.externalHttpCircuitBreaker(properties));

        GithubProperties githubProperties = new GithubProperties();
        githubProperties.setBaseUrl(wireMock.baseUrl());

        ScrapperMetricsService metricsService = mock(ScrapperMetricsService.class);

        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        return new GithubClient(restTemplate, githubProperties, executor, metricsService);
    }

    private ResilienceProperties defaultProperties() {
        ResilienceProperties properties = new ResilienceProperties();

        properties.getTimeout().setConnectTimeout(Duration.ofMillis(200));
        properties.getTimeout().setReadTimeout(Duration.ofMillis(500));

        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setWaitDuration(Duration.ofMillis(50));
        properties.getRetry().setBackoffStrategy(ResilienceProperties.BackoffStrategy.CONSTANT);
        properties.getRetry().setRetryableStatuses(java.util.List.of(500, 502, 503, 504, 408, 429));

        properties.getCircuitBreaker().setSlidingWindowSize(10);
        properties.getCircuitBreaker().setMinimumNumberOfCalls(5);
        properties.getCircuitBreaker().setFailureRateThreshold(50);
        properties.getCircuitBreaker().setPermittedCallsInHalfOpenState(3);
        properties.getCircuitBreaker().setWaitDurationInOpenState(Duration.ofSeconds(5));

        return properties;
    }

    private String successBody() {
        return """
                [
                  {
                    "title": "test issue",
                    "body": "test body",
                    "created_at": "%s",
                    "user": {
                      "login": "tester"
                    }
                  }
                ]
                """.formatted(Instant.now());
    }
}
