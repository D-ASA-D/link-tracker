package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.BatchProcessReport;
import backend.academy.linktracker.scrapper.dto.FailedLinkCheck;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.DefaultLinkUpdateService;
import backend.academy.linktracker.scrapper.service.SingleLinkUpdateProcessor;
import backend.academy.linktracker.scrapper.service.UpdateSender;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultLinkUpdateServiceTest {

    private LinkRepository linkRepository;
    private UpdateSender updateSender;
    private SchedulerProperties schedulerProperties;
    private ExecutorService executorService;
    private SingleLinkUpdateProcessor processor;
    private ScrapperMetricsService metricsService;

    private DefaultLinkUpdateService service;

    @BeforeEach
    void setup() {
        linkRepository = mock(LinkRepository.class);
        updateSender = mock(UpdateSender.class);
        processor = mock(SingleLinkUpdateProcessor.class);
        metricsService = mock(ScrapperMetricsService.class);
        mockMetricsService();

        schedulerProperties = new SchedulerProperties();
        schedulerProperties.setBatchSize(100);
        schedulerProperties.setThreads(2);
        schedulerProperties.setRecheckAge(Duration.ofSeconds(30));

        executorService = Executors.newFixedThreadPool(2);

        service = new DefaultLinkUpdateService(
                linkRepository, updateSender, schedulerProperties, executorService, processor, metricsService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void processDueLinks_shouldProcessSuccessfulBatch() {
        LinkRecord link1 = link(1L, "https://github.com/user/repo1");
        LinkRecord link2 = link(2L, "https://github.com/user/repo2");

        when(linkRepository.findLinksDueForUpdateCheck(any(), anyInt())).thenReturn(List.of(link1, link2), List.of());

        when(processor.processOneLink(link1)).thenReturn(Optional.empty());
        when(processor.processOneLink(link2)).thenReturn(Optional.empty());

        BatchProcessReport report = service.processDueLinks();

        assertEquals(2, report.processed());
        assertEquals(2, report.succeeded());
        assertEquals(0, report.failed());
        assertEquals(0, report.failedLinks().size());

        verify(processor).processOneLink(link1);
        verify(processor).processOneLink(link2);
        verify(updateSender, never()).send(any());
    }

    @Test
    void processDueLinks_shouldContinueProcessing_whenOneLinkFails() {
        LinkRecord failedLink = link(1L, "https://github.com/user/failed");
        LinkRecord successfulLink = link(2L, "https://github.com/user/success");

        FailedLinkCheck failure = new FailedLinkCheck(1L, failedLink.url(), "github unavailable", List.of(100L));

        when(linkRepository.findLinksDueForUpdateCheck(any(), anyInt()))
                .thenReturn(List.of(failedLink, successfulLink), List.of());

        when(processor.processOneLink(failedLink)).thenReturn(Optional.of(failure));
        when(processor.processOneLink(successfulLink)).thenReturn(Optional.empty());

        BatchProcessReport report = service.processDueLinks();

        assertEquals(2, report.processed());
        assertEquals(1, report.succeeded());
        assertEquals(1, report.failed());
        assertEquals(1, report.failedLinks().size());

        verify(processor).processOneLink(failedLink);
        verify(processor).processOneLink(successfulLink);
    }

    @Test
    void processDueLinks_shouldSendFailureReport_whenLinkFailed() {
        LinkRecord failedLink = link(1L, "https://github.com/user/failed");

        FailedLinkCheck failure = new FailedLinkCheck(1L, failedLink.url(), "github unavailable", List.of(100L));

        when(linkRepository.findLinksDueForUpdateCheck(any(), anyInt())).thenReturn(List.of(failedLink), List.of());

        when(processor.processOneLink(failedLink)).thenReturn(Optional.of(failure));

        service.processDueLinks();

        verify(updateSender)
                .send(argThat(update -> update.id().equals(-1L)
                        && update.tgChatIds().equals(List.of(100L))
                        && update.description().contains("Не удалось проверить некоторые ссылки")
                        && update.description().contains(failedLink.url())
                        && update.description().contains("github unavailable")));
    }

    @Test
    void processDueLinks_shouldProcessSeveralBatches() {
        LinkRecord link1 = link(1L, "https://github.com/user/repo1");
        LinkRecord link2 = link(2L, "https://github.com/user/repo2");

        when(linkRepository.findLinksDueForUpdateCheck(any(), anyInt()))
                .thenReturn(List.of(link1), List.of(link2), List.of());

        when(processor.processOneLink(link1)).thenReturn(Optional.empty());
        when(processor.processOneLink(link2)).thenReturn(Optional.empty());

        BatchProcessReport report = service.processDueLinks();

        assertEquals(2, report.processed());
        assertEquals(2, report.succeeded());
        assertEquals(0, report.failed());

        verify(linkRepository, times(3)).findLinksDueForUpdateCheck(any(), eq(100));
    }

    private void mockMetricsService() {
        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
    }

    private LinkRecord link(Long id, String url) {
        return new LinkRecord(id, url, Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"));
    }
}
