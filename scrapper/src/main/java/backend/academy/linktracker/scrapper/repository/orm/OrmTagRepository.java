package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.TagRecord;
import backend.academy.linktracker.scrapper.model.orm.SubscriptionTagId;
import backend.academy.linktracker.scrapper.model.orm.TagEntity;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmTagRepository implements TagRepository {

    private final JpaTagRepository tagRepository;
    private final JpaSubscriptionTagRepository subscriptionTagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public TagRecord save(String name) {
        try {
            TagEntity entity = new TagEntity();
            entity.setName(name);
            return map(tagRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Tag already exists");
        }
    }

    @Override
    public Optional<TagRecord> findById(Long id) {
        return tagRepository.findById(id).map(this::map);
    }

    @Override
    public Optional<TagRecord> findByName(String name) {
        return tagRepository.findByName(name).map(this::map);
    }

    @Override
    public TagRecord findOrCreate(String name) {
        return findByName(name).orElseGet(() -> {
            try {
                return save(name);
            } catch (IllegalStateException e) {
                return findByName(name).orElseThrow();
            }
        });
    }

    @Override
    public List<TagRecord> findAll(int limit, int offset) {
        return entityManager
                .createQuery("""
                select t
                from TagEntity t
                order by t.id asc
                """, TagEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @Override
    public TagRecord update(Long id, String name) {
        try {
            int updated = tagRepository.updateNameById(id, name);
            if (updated == 0) {
                throw new java.util.NoSuchElementException("Tag not found");
            }
            return findById(id).orElseThrow();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Tag already exists");
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        tagRepository.deleteById(id);
    }

    @Override
    public List<TagRecord> findBySubscriptionId(Long subscriptionId) {
        return tagRepository.findAllBySubscriptionId(subscriptionId).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @Override
    public void attachToSubscription(Long subscriptionId, Long tagId) {
        subscriptionTagRepository.insertIfNotExists(subscriptionId, tagId);
    }

    @Override
    @Transactional
    public void detachAllFromSubscription(Long subscriptionId) {
        subscriptionTagRepository.deleteBySubscriptionId(subscriptionId);
    }

    @Override
    public boolean isAttached(Long subscriptionId, Long tagId) {
        return subscriptionTagRepository.existsById(new SubscriptionTagId(subscriptionId, tagId));
    }

    @Override
    @Transactional
    public void detachFromSubscription(Long subscriptionId, Long tagId) {
        subscriptionTagRepository.deleteById(new SubscriptionTagId(subscriptionId, tagId));
    }

    @Override
    public long countUsages(Long tagId) {
        return subscriptionTagRepository.countByTagId(tagId);
    }

    private TagRecord map(TagEntity entity) {
        return new TagRecord(entity.getId(), entity.getName());
    }
}
