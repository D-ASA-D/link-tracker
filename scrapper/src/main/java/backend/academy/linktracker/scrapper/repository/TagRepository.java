package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TagRecord;
import java.util.List;
import java.util.Optional;

public interface TagRepository {

    TagRecord save(String name);

    Optional<TagRecord> findById(Long id);

    Optional<TagRecord> findByName(String name);

    TagRecord findOrCreate(String name);

    List<TagRecord> findAll(int limit, int offset);

    TagRecord update(Long id, String name);

    void deleteById(Long id);

    boolean isAttached(Long subscriptionId, Long tagId);

    void detachFromSubscription(Long subscriptionId, Long tagId);

    long countUsages(Long tagId);

    List<TagRecord> findBySubscriptionId(Long subscriptionId);

    void attachToSubscription(Long subscriptionId, Long tagId);

    void detachAllFromSubscription(Long subscriptionId);
}
