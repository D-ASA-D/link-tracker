package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.LinkClientResolver;
import backend.academy.linktracker.scrapper.dto.FailedLinkCheck;
import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.service.SingleLinkUpdateProcessor;
import backend.academy.linktracker.scrapper.service.UpdateMessageFormatter;
import backend.academy.linktracker.scrapper.service.UpdateSender;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SingleLinkUpdateProcessorTest {

    private SubscriptionRepository subscriptionRepository;
    private LinkClientResolver resolver;
    private LinkRepository linkRepository;
    private UpdateMessageFormatter formatter;
    private UpdateSender updateSender;
    private ScrapperMetricsService metricsService;

    private SingleLinkUpdateProcessor processor;

    @BeforeEach
    void setup() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        resolver = mock(LinkClientResolver.class);
        linkRepository = mock(LinkRepository.class);
        formatter = mock(UpdateMessageFormatter.class);
        updateSender = mock(UpdateSender.class);
        metricsService = mock(ScrapperMetricsService.class);

        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        processor = new SingleLinkUpdateProcessor(
                subscriptionRepository, resolver, linkRepository, formatter, updateSender, metricsService);
    }

    @Test
    void processOneLink_shouldSendUpdate_whenLinkWasUpdated() {
        LinkRecord link = link(1L, "https://github.com/user/repo", "2026-03-01T00:00:00Z");

        LinkUpdateInfo info = new LinkUpdateInfo(
                Instant.parse("2026-03-08T10:00:00Z"),
                "New issue",
                "octocat",
                Instant.parse("2026-03-08T10:00:00Z"),
                "Issue preview",
                "ISSUE");

        when(subscriptionRepository.findByLinkId(1L)).thenReturn(List.of(new SubscriptionRecord(10L, 100L, 1L)));

        when(resolver.resolve(link.url())).thenReturn(Optional.of(info));
        when(formatter.format(link.url(), info)).thenReturn("Formatted update");

        Optional<FailedLinkCheck> result = processor.processOneLink(link);

        assertTrue(result.isEmpty());

        verify(updateSender).send(new RawLinkUpdate(1L, "Formatted update", "octocat", List.of(100L)));
    }

    @Test
    void processOneLink_shouldNotSendUpdate_whenResolverReturnsEmpty() {
        LinkRecord link = link(1L, "https://github.com/user/repo", "2026-03-01T00:00:00Z");

        when(subscriptionRepository.findByLinkId(1L)).thenReturn(List.of());
        when(resolver.resolve(link.url())).thenReturn(Optional.empty());

        Optional<FailedLinkCheck> result = processor.processOneLink(link);

        assertTrue(result.isEmpty());

        verify(updateSender, never()).send(any());
    }

    @Test
    void processOneLink_shouldNotSendUpdate_whenEventTimeIsNotAfterLastUpdated() {
        LinkRecord link = link(1L, "https://github.com/user/repo", "2026-03-08T10:00:00Z");

        LinkUpdateInfo info = new LinkUpdateInfo(
                Instant.parse("2026-03-08T10:00:00Z"),
                "New issue",
                "octocat",
                Instant.parse("2026-03-08T10:00:00Z"),
                "Issue preview",
                "ISSUE");

        when(subscriptionRepository.findByLinkId(1L)).thenReturn(List.of());
        when(resolver.resolve(link.url())).thenReturn(Optional.of(info));

        Optional<FailedLinkCheck> result = processor.processOneLink(link);

        assertTrue(result.isEmpty());

        verify(updateSender, never()).send(any());
    }

    @Test
    void processOneLink_shouldReturnFailure_whenResolverThrows() {
        LinkRecord link = link(1L, "https://github.com/user/failed", "2026-03-01T00:00:00Z");

        when(subscriptionRepository.findByLinkId(1L)).thenReturn(List.of(new SubscriptionRecord(10L, 100L, 1L)));

        when(resolver.resolve(link.url())).thenThrow(new RuntimeException("github unavailable"));

        Optional<FailedLinkCheck> result = processor.processOneLink(link);

        assertTrue(result.isPresent());
        assertEquals(1L, result.orElseThrow().linkId());
        assertEquals(link.url(), result.orElseThrow().url());
        assertEquals("github unavailable", result.orElseThrow().reason());
        assertEquals(List.of(100L), result.orElseThrow().chatIds());

        verify(updateSender, never()).send(any());
    }

    @Test
    void processOneLink_shouldReturnFailure_whenSendFails() {
        LinkRecord link = link(1L, "https://github.com/user/repo", "2026-03-01T00:00:00Z");

        LinkUpdateInfo info = new LinkUpdateInfo(
                Instant.parse("2026-03-08T10:00:00Z"),
                "New issue",
                "octocat",
                Instant.parse("2026-03-08T10:00:00Z"),
                "Issue preview",
                "ISSUE");

        when(subscriptionRepository.findByLinkId(1L)).thenReturn(List.of(new SubscriptionRecord(10L, 100L, 1L)));

        when(resolver.resolve(link.url())).thenReturn(Optional.of(info));
        when(formatter.format(link.url(), info)).thenReturn("Formatted update");

        doThrow(new RuntimeException("kafka unavailable")).when(updateSender).send(any(RawLinkUpdate.class));

        Optional<FailedLinkCheck> result = processor.processOneLink(link);

        assertTrue(result.isPresent());
        assertEquals("kafka unavailable", result.orElseThrow().reason());
    }

    private LinkRecord link(Long id, String url, String lastUpdated) {
        return new LinkRecord(id, url, Instant.parse(lastUpdated), Instant.parse("2026-03-01T00:00:00Z"));
    }
}
