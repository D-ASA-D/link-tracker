package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.LinkClientResolver;
import backend.academy.linktracker.scrapper.dto.FailedLinkCheck;
import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleLinkUpdateProcessor {

    private static final String UNKNOWN_AUTHOR = "unknown";
    private static final String DATABASE_SCOPE = "database";
    private static final String LINKS_TABLE = "links";
    private static final String SUBSCRIPTIONS_TABLE = "subscriptions";

    private final SubscriptionRepository subscriptionRepository;
    private final LinkClientResolver resolver;
    private final LinkRepository linkRepository;
    private final UpdateMessageFormatter formatter;
    private final UpdateSender updateSender;
    private final ScrapperMetricsService metricsService;

    @Transactional
    public Optional<FailedLinkCheck> processOneLink(LinkRecord link) {
        Instant now = Instant.now();

        List<Long> chatIds =
                database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.findByLinkId(link.id())).stream()
                        .map(subscription -> subscription.chatId())
                        .distinct()
                        .toList();

        try {
            Optional<LinkUpdateInfo> infoOptional = resolver.resolve(link.url());

            if (infoOptional.isEmpty()) {
                return Optional.empty();
            }

            LinkUpdateInfo info = infoOptional.orElseThrow();

            if (info.eventTime() == null) {
                return Optional.empty();
            }

            if (link.lastUpdated() == null || info.eventTime().isAfter(link.lastUpdated())) {
                if (!chatIds.isEmpty()) {
                    sendUpdate(link, info, chatIds);
                }

                database(LINKS_TABLE, () -> linkRepository.updateLastUpdated(link.id(), info.eventTime()));
                log.info("link_updated id={} url={}", link.id(), link.url());
            }

            return Optional.empty();
        } catch (Exception e) {
            String reason = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            log.error("link_processing_error url={}", link.url(), e);
            return Optional.of(new FailedLinkCheck(link.id(), link.url(), reason, chatIds));
        } finally {
            database(LINKS_TABLE, () -> linkRepository.updateLastCheckedAt(link.id(), now));
        }
    }

    private void sendUpdate(LinkRecord link, LinkUpdateInfo info, List<Long> chatIds) {
        String description = formatter.format(link.url(), info);
        String author = info.username() == null || info.username().isBlank() ? UNKNOWN_AUTHOR : info.username();
        updateSender.send(new RawLinkUpdate(link.id(), description, author, chatIds));
    }

    private <T> T database(String table, Supplier<T> action) {
        return metricsService.recordRequestDuration(DATABASE_SCOPE, table, action);
    }

    private void database(String table, Runnable action) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, table, action);
    }
}
