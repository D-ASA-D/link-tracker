package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.RenameTagRequest;
import backend.academy.linktracker.scrapper.dto.RenameTagResponse;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.model.TagRecord;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagManagementServiceImpl implements TagManagementService {

    private static final String DATABASE_SCOPE = "database";
    private static final String TAGS_TABLE = "tags";
    private static final String SUBSCRIPTIONS_TABLE = "subscriptions";
    private static final String SUBSCRIPTION_TAGS_TABLE = "subscription_tags";

    private final SubscriptionRepository subscriptionRepository;
    private final TagRepository tagRepository;
    private final ScrapperMetricsService metricsService;

    @Override
    @Transactional
    public RenameTagResponse renameTag(Long chatId, RenameTagRequest request) {
        String oldName = normalize(request.oldName());
        String newName = normalize(request.newName());

        if (oldName.equals(newName)) {
            throw new IllegalArgumentException("Old tag and new tag must be different");
        }

        TagRecord oldTag = database(TAGS_TABLE, () -> tagRepository.findByName(oldName))
                .orElseThrow(() -> new NoSuchElementException("Tag not found"));

        List<SubscriptionRecord> subscriptions =
                database(SUBSCRIPTIONS_TABLE, () -> subscriptionRepository.findByChatIdAndTag(chatId, oldName));

        if (subscriptions.isEmpty()) {
            throw new NoSuchElementException("Tag is not attached to any links of this chat");
        }

        TagRecord newTag = database(TAGS_TABLE, () -> tagRepository.findOrCreate(newName));

        int updated = 0;
        for (SubscriptionRecord subscription : subscriptions) {
            boolean attached =
                    database(SUBSCRIPTION_TAGS_TABLE, () -> tagRepository.isAttached(subscription.id(), newTag.id()));

            if (!attached) {
                database(
                        SUBSCRIPTION_TAGS_TABLE,
                        () -> tagRepository.attachToSubscription(subscription.id(), newTag.id()));
            }

            database(
                    SUBSCRIPTION_TAGS_TABLE,
                    () -> tagRepository.detachFromSubscription(subscription.id(), oldTag.id()));
            updated++;
        }

        long usages = database(SUBSCRIPTION_TAGS_TABLE, () -> tagRepository.countUsages(oldTag.id()));

        if (usages == 0) {
            database(TAGS_TABLE, () -> tagRepository.deleteById(oldTag.id()));
        }

        return new RenameTagResponse(oldName, newName, updated);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        return value.trim();
    }

    private <T> T database(String table, Supplier<T> action) {
        return metricsService.recordRequestDuration(DATABASE_SCOPE, table, action);
    }

    private void database(String table, Runnable action) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, table, action);
    }
}
