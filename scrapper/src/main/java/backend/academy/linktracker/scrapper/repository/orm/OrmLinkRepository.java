package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.model.orm.LinkEntity;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmLinkRepository implements LinkRepository {

    private final JpaLinkRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public LinkRecord save(String url, Instant lastUpdated, Instant lastCheckedAt) {
        try {
            LinkEntity entity = new LinkEntity();
            entity.setUrl(url);
            entity.setLastUpdated(lastUpdated);
            entity.setLastCheckedAt(lastCheckedAt == null ? Instant.now() : lastCheckedAt);
            return map(repository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Link already exists");
        }
    }

    @Override
    public Optional<LinkRecord> findByUrl(String url) {
        return repository.findByUrl(url).map(this::map);
    }

    @Override
    public Optional<LinkRecord> findById(Long id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<LinkRecord> findPage(int limit, int offset) {
        return entityManager
                .createQuery("""
                select l
                from LinkEntity l
                order by l.id asc
                """, LinkEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<LinkRecord> findLinksDueForUpdateCheck(Instant checkedBefore, int limit) {
        return entityManager
                .createQuery("""
                select l
                from LinkEntity l
                where l.lastCheckedAt < :checkedBefore
                order by l.lastCheckedAt asc, l.id asc
                """, LinkEntity.class)
                .setParameter("checkedBefore", checkedBefore)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @Override
    public void updateLastUpdated(Long id, Instant lastUpdated) {
        repository.updateLastUpdatedById(id, lastUpdated);
    }

    @Transactional
    @Override
    public void updateLastCheckedAt(Long id, Instant lastCheckedAt) {
        repository.updateLastCheckedAtById(id, lastCheckedAt);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long countByUrlContaining(String domain) {
        return repository.countByUrlContaining(domain);
    }

    private LinkRecord map(LinkEntity entity) {
        return new LinkRecord(entity.getId(), entity.getUrl(), entity.getLastUpdated(), entity.getLastCheckedAt());
    }
}
