package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.DeleteLinksByTagResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final String DATABASE_SCOPE = "database";
    private static final String CHATS_TABLE = "chats";
    private static final String LINKS_TABLE = "links";
    private static final String SUBSCRIPTIONS_TABLE = "subscriptions";
    private static final String TAGS_TABLE = "tags";
    private static final String SUBSCRIPTION_TAGS_TABLE = "subscription_tags";

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TagRepository tagRepository;
    private final ScrapperMetricsService metricsService;

    public void registerChat(Long chatId) {
        database(CHATS_TABLE, () -> chatRepository.register(chatId));
        log.info("Chat registered chatId={}", sanitize(chatId));
    }

    public void unregisterChat(Long chatId) {
        database(CHATS_TABLE, () -> chatRepository.unregister(chatId));
        log.info("Chat unregistered chatId={}", sanitize(chatId));
    }

    @Transactional
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        boolean chatExists = database(CHATS_TABLE, () -> chatRepository.exists(chatId));

        if (!chatExists) {
            throw new NoSuchElementException("Chat not registered");
        }

        String normalizedUrl = normalizeUrl(request.link());

        LinkRecord link = database(LINKS_TABLE, () -> linkRepository.findByUrl(normalizedUrl))
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return database(LINKS_TABLE, () -> linkRepository.save(normalizedUrl, null, now));
                });

        boolean alreadyTracked =
                database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.existsByChatIdAndLinkId(chatId, link.id()));

        if (alreadyTracked) {
            throw new IllegalStateException("Link already tracked");
        }

        SubscriptionRecord subscription =
                database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.save(chatId, link.id()));

        List<String> tags = normalizeTags(request.tags());
        for (String tagName : tags) {
            var tag = database(TAGS_TABLE, () -> tagRepository.findOrCreate(tagName));
            database(SUBSCRIPTION_TAGS_TABLE, () -> tagRepository.attachToSubscription(subscription.id(), tag.id()));
        }

        return map(link, subscription.id());
    }

    @Transactional
    public LinkResponse removeLink(Long chatId, RemoveLinkRequest request) {
        boolean chatExists = database(CHATS_TABLE, () -> chatRepository.exists(chatId));

        if (!chatExists) {
            throw new NoSuchElementException("Chat not found");
        }

        String normalizedUrl = normalizeUrl(request.link());

        LinkRecord link = database(LINKS_TABLE, () -> linkRepository.findByUrl(normalizedUrl))
                .orElseThrow(() -> new NoSuchElementException("Link not found"));

        SubscriptionRecord subscription = database(
                        SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.findByChatIdAndLinkId(chatId, link.id()))
                .orElseThrow(() -> new NoSuchElementException("Link not found"));

        LinkResponse response = map(link, subscription.id());

        database(SUBSCRIPTION_TAGS_TABLE, () -> tagRepository.detachAllFromSubscription(subscription.id()));
        database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.deleteById(subscription.id()));

        long subscriptionsCount = database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.countByLinkId(link.id()));

        if (subscriptionsCount == 0) {
            database(LINKS_TABLE, () -> linkRepository.deleteById(link.id()));
        }

        return response;
    }

    public ListLinksResponse listLinks(Long chatId) {
        return listLinks(chatId, DEFAULT_PAGE_SIZE, 0);
    }

    public ListLinksResponse listLinks(Long chatId, int limit, int offset) {
        boolean chatExists = database(CHATS_TABLE, () -> chatRepository.exists(chatId));

        if (!chatExists) {
            throw new NoSuchElementException("Chat not found");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }

        List<LinkResponse> links =
                database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.findByChatId(chatId, limit, offset)).stream()
                        .map(subscription -> {
                            LinkRecord link = database(
                                            LINKS_TABLE, () -> linkRepository.findById(subscription.linkId()))
                                    .orElseThrow(() -> new NoSuchElementException("Link not found"));

                            return map(link, subscription.id());
                        })
                        .toList();

        return new ListLinksResponse(links, links.size());
    }

    private LinkResponse map(LinkRecord link, Long subscriptionId) {
        List<String> tags = database(TAGS_TABLE, () -> tagRepository.findBySubscriptionId(subscriptionId)).stream()
                .map(tag -> tag.name())
                .toList();

        return new LinkResponse(link.id(), link.url(), new ArrayList<>(tags), List.of());
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Link must not be null");
        }

        String normalized = url.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Link must not be blank");
        }

        return normalized;
    }

    @Transactional
    public DeleteLinksByTagResponse removeLinksByTag(Long chatId, String tag) {
        String normalizedTag = normalizeTag(tag);

        List<SubscriptionRecord> subscriptions =
                database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.findByChatIdAndTag(chatId, normalizedTag));

        if (subscriptions.isEmpty()) {
            throw new NoSuchElementException("No links found for tag: " + normalizedTag);
        }

        int removed = 0;

        for (SubscriptionRecord subscription : subscriptions) {
            Long subscriptionId = subscription.id();
            Long linkId = subscription.linkId();

            database(SUBSCRIPTION_TAGS_TABLE, () -> tagRepository.detachAllFromSubscription(subscriptionId));
            database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.deleteById(subscriptionId));

            long subscriptionsCount = database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.countByLinkId(linkId));

            if (subscriptionsCount == 0) {
                database(LINKS_TABLE, () -> linkRepository.deleteById(linkId));
            }

            removed++;
        }

        database(TAGS_TABLE, () -> tagRepository.findByName(normalizedTag)).ifPresent(tagRecord -> {
            long usages = database(SUBSCRIPTION_TAGS_TABLE, () -> tagRepository.countUsages(tagRecord.id()));

            if (usages == 0) {
                database(TAGS_TABLE, () -> tagRepository.deleteById(tagRecord.id()));
            }
        });

        return new DeleteLinksByTagResponse(normalizedTag, removed);
    }

    private String normalizeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag must not be blank");
        }
        return tag.trim();
    }

    private String sanitize(Object value) {
        return value == null ? null : String.valueOf(value).replace("\n", "").replace("\r", "");
    }

    private <T> T database(String table, Supplier<T> action) {
        return metricsService.recordRequestDuration(DATABASE_SCOPE, table, action);
    }

    private void database(String table, Runnable action) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, table, action);
    }
}
