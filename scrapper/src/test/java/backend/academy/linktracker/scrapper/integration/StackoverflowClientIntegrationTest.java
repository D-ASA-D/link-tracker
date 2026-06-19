package backend.academy.linktracker.scrapper.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.ResilientHttpExecutor;
import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.configuration.ClientConfiguration;
import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StackoverflowClientIntegrationTest.TestConfig.class)
@EnableWireMock
@TestPropertySource(properties = {"app.stackoverflow.key=test-key", "app.stackoverflow.access-token=test-token"})
class StackoverflowClientIntegrationTest {

    @TestConfiguration
    @Import({
        StackoverflowClient.class,
        ResilientHttpExecutor.class,
        ResilienceConfiguration.class,
        ClientConfiguration.class
    })
    static class TestConfig {

        @Bean
        StackoverflowProperties stackoverflowProperties() {
            return new StackoverflowProperties();
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
    private StackoverflowClient stackoverflowClient;

    @Autowired
    private StackoverflowProperties stackoverflowProperties;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @BeforeEach
    void setupProperties() {
        stackoverflowProperties.setBaseUrl(environment.getProperty("wiremock.server.baseUrl"));
        stackoverflowProperties.setKey("test-key");
        stackoverflowProperties.setAccessToken("test-token");
    }

    @Test
    void shouldReturnLatestAnswerInfoFromWireMock() {
        stubFor(get(urlPathEqualTo("/questions/12345")).willReturn(okJson("""
            {
              "items": [
                {
                  "title": "How to use Liquibase?"
                }
              ]
            }
            """)));

        stubFor(get(urlPathEqualTo("/questions/12345/answers"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("filter", equalTo("withbody"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson("""
            {
              "items": [
                {
                  "creation_date": 1772964000,
                  "body": "Very long answer body",
                  "owner": {
                    "display_name": "Jon Skeet"
                  }
                }
              ]
            }
            """)));

        stubFor(get(urlPathEqualTo("/questions/12345/comments"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("filter", equalTo("withbody"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson("""
            {
              "items": []
            }
            """)));

        Optional<LinkUpdateInfo> result =
                stackoverflowClient.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isPresent());

        LinkUpdateInfo info = result.orElseThrow();
        assertEquals("How to use Liquibase?", info.title());
        assertEquals("Jon Skeet", info.username());
        assertEquals(Instant.ofEpochSecond(1772964000L), info.eventTime());
        assertEquals("Very long answer body", info.preview());
        assertEquals("ANSWER", info.eventType());
    }

    @Test
    void shouldReturnEmptyWhenNoItems() {
        stubFor(get(urlPathEqualTo("/questions/12345")).willReturn(okJson("""
                {
                  "items": []
                }
                """)));

        Optional<LinkUpdateInfo> result =
                stackoverflowClient.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenApiReturnsInvalidPayload() {
        stubFor(get(urlPathEqualTo("/questions/12345")).willReturn(okJson("""
                {
                  "unexpected": []
                }
                """)));

        Optional<LinkUpdateInfo> result =
                stackoverflowClient.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnPlainTextPreviewWithoutHtml() {
        stubFor(get(urlPathEqualTo("/questions/12345")).willReturn(okJson("""
        {
          "items": [
            {
              "title": "How to use Liquibase?"
            }
          ]
        }
        """)));

        stubFor(get(urlPathEqualTo("/questions/12345/answers"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("filter", equalTo("withbody"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson("""
        {
          "items": [
            {
              "creation_date": 1772964000,
              "body": "<p>Hello <a href=\\"https://example.com\\">world</a></p>",
              "owner": {
                "display_name": "Jon Skeet"
              }
            }
          ]
        }
        """)));

        stubFor(get(urlPathEqualTo("/questions/12345/comments"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("filter", equalTo("withbody"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson("""
        {
          "items": []
        }
        """)));

        Optional<LinkUpdateInfo> result =
                stackoverflowClient.getLastUpdate("https://stackoverflow.com/questions/12345/example");

        assertTrue(result.isPresent());
        assertEquals("Hello world", result.orElseThrow().preview());
    }
}
