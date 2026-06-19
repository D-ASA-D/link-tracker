package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.BatchProcessReport;
import backend.academy.linktracker.scrapper.dto.FailedLinkCheck;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLinkUpdateService implements LinkUpdateService {

    private static final long FAILURE_REPORT_LINK_ID = -1L;
    private static final String DATABASE_SCOPE = "database";
    private static final String LINKS_TABLE = "links";

    private final LinkRepository linkRepository;
    private final UpdateSender updateSender;
    private final SchedulerProperties schedulerProperties;
    private final ExecutorService linkUpdateExecutor;
    private final SingleLinkUpdateProcessor processor;
    private final ScrapperMetricsService metricsService;

    @Override
    public BatchProcessReport processDueLinks() {
        Instant checkedBefore = Instant.now().minus(schedulerProperties.getRecheckAge());

        int totalProcessed = 0;
        int totalSucceeded = 0;
        int totalFailed = 0;
        List<FailedLinkCheck> allFailures = new ArrayList<>();

        while (true) {
            List<LinkRecord> batch = metricsService.recordRequestDuration(
                    DATABASE_SCOPE,
                    LINKS_TABLE,
                    () -> linkRepository.findLinksDueForUpdateCheck(checkedBefore, schedulerProperties.getBatchSize()));

            if (batch.isEmpty()) {
                break;
            }

            BatchProcessReport report = processBatch(batch);

            totalProcessed += report.processed();
            totalSucceeded += report.succeeded();
            totalFailed += report.failed();
            allFailures.addAll(report.failedLinks());

            log.info(
                    "batch_processed processed={} succeeded={} failed={}",
                    report.processed(),
                    report.succeeded(),
                    report.failed());
        }

        BatchProcessReport finalReport =
                new BatchProcessReport(totalProcessed, totalSucceeded, totalFailed, List.copyOf(allFailures));

        if (!allFailures.isEmpty()) {
            log.warn("failed_links={}", allFailures);
            sendFailureReport(finalReport);
        }

        return finalReport;
    }

    private BatchProcessReport processBatch(List<LinkRecord> batch) {
        int threads = Math.max(1, schedulerProperties.getThreads());
        List<List<LinkRecord>> chunks = splitIntoChunks(batch, threads);

        List<CompletableFuture<List<FailedLinkCheck>>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> processChunk(chunk), linkUpdateExecutor))
                .toList();

        List<FailedLinkCheck> failures = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        int failed = failures.size();
        int processed = batch.size();
        int succeeded = processed - failed;

        return new BatchProcessReport(processed, succeeded, failed, failures);
    }

    private List<FailedLinkCheck> processChunk(List<LinkRecord> chunk) {
        List<FailedLinkCheck> failures = new ArrayList<>();

        for (LinkRecord link : chunk) {
            processor.processOneLink(link).ifPresent(failures::add);
        }

        return failures;
    }

    private List<List<LinkRecord>> splitIntoChunks(List<LinkRecord> batch, int chunksCount) {
        if (batch.isEmpty()) {
            return List.of();
        }

        int actualChunks = Math.min(chunksCount, batch.size());
        int chunkSize = (int) Math.ceil((double) batch.size() / actualChunks);

        List<List<LinkRecord>> chunks = new ArrayList<>();
        for (int i = 0; i < batch.size(); i += chunkSize) {
            chunks.add(batch.subList(i, Math.min(i + chunkSize, batch.size())));
        }

        return chunks;
    }

    private void sendFailureReport(BatchProcessReport report) {
        if (report.failedLinks().isEmpty()) {
            return;
        }

        Map<Long, List<FailedLinkCheck>> failuresByChatId = new LinkedHashMap<>();

        for (FailedLinkCheck failed : report.failedLinks()) {
            for (Long chatId : failed.chatIds()) {
                failuresByChatId
                        .computeIfAbsent(chatId, ignored -> new ArrayList<>())
                        .add(failed);
            }
        }

        for (Map.Entry<Long, List<FailedLinkCheck>> entry : failuresByChatId.entrySet()) {
            Long chatId = entry.getKey();
            List<FailedLinkCheck> failedLinks = entry.getValue();

            String description = buildFailureReportMessage(failedLinks);

            try {
                updateSender.send(new RawLinkUpdate(FAILURE_REPORT_LINK_ID, description, "scrapper", List.of(chatId)));
            } catch (Exception e) {
                log.error("failure_report_send_error chatId={}", chatId, e);
            }
        }
    }

    private String buildFailureReportMessage(List<FailedLinkCheck> failedLinks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Не удалось проверить некоторые ссылки:\n");

        for (FailedLinkCheck failed : failedLinks) {
            sb.append("\n")
                    .append("Ссылка: ")
                    .append(failed.url())
                    .append("\n")
                    .append("Причина: ")
                    .append(failed.reason())
                    .append("\n");
        }

        return sb.toString().trim();
    }
}
